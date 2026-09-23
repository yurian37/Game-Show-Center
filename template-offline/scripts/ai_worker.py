#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI Worker for Game Show Center (Template Offline)
Integrates local multimodal Qwen 3.5B (GGUF) + Wikimedia Search
"""

import sys
import os
import re
import json
import time
import argparse
from io import BytesIO

# Force UTF-8 stdout/stderr for Windows compatibility
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

# Prohibited themes keywords (Sex, Drugs, Weapons)
PROHIBITED_KEYWORDS = [
    # Sex / Pornography / Erotic
    r"\bsexo\b", r"\bsexual\b", r"\bsexuales\b", r"\bpornografia\b", r"\bpornografía\b",
    r"\bporno\b", r"\berotico\b", r"\berótico\b", r"\berotica\b", r"\berótica\b",
    r"\bdesnudos?\b", r"\bgenitales?\b", r"\bprostitucion\b", r"\bprostitución\b",
    r"\bsex\b", r"\bporn\b", r"\bpornography\b", r"\berotic\b", r"\bnude\b", r"\bnudity\b",
    # Drugs / Narcotics
    r"\bdrogas?\b", r"\bnarcoticos?\b", r"\bnarcóticos?\b", r"\bcocaina\b", r"\bcocaína\b",
    r"\bheroina\b", r"\bheroína\b", r"\bmarihuana\b", r"\bmetanfetamina\b", r"\bfentanilo\b",
    r"\bestupefacientes?\b", r"\bdrugs?\b", r"\bnarcotics?\b", r"\bcocaine\b", r"\bheroin\b",
    r"\bmarijuana\b", r"\bweed\b", r"\bmeth\b", r"\bfentanyl\b",
    # Weapons / Firearms / Bombs
    r"\barmas?\b", r"\bpistolas?\b", r"\brifles?\b", r"\bbombas?\b", r"\bexplosivos?\b",
    r"\bfusiles?\b", r"\bmetralletas?\b", r"\barmamento\b", r"\bgranadas?\b",
    r"\bweapons?\b", r"\bguns?\b", r"\bpistols?\b", r"\brifles?\b", r"\bbombs?\b",
    r"\bexplosives?\b", r"\bfirearms?\b", r"\bammo\b", r"\bammunition\b"
]

def check_prohibited_theme(prompt: str):
    """Checks if the prompt contains prohibited themes (Sex, Drugs, Weapons)."""
    text_lower = prompt.lower()
    for pattern in PROHIBITED_KEYWORDS:
        if re.search(pattern, text_lower, re.IGNORECASE):
            return True
    return False

def report_progress(current: int, total: int, step: str = ""):
    """Emits a real-time progress update line to stdout for the Java UI."""
    pct = round(current / total, 3) if total > 0 else 0.0
    payload = {
        "type": "progress",
        "current": current,
        "total": total,
        "percent": min(1.0, max(0.0, pct)),
        "step": step
    }
    sys.stdout.write(f"PROGRESS:{json.dumps(payload, ensure_ascii=False)}\n")
    sys.stdout.flush()

def find_model_paths():

    """Locates the GGUF model and multimodal projector."""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        # Relative to script (template-offline/scripts -> template-offline/LLM Model)
        os.path.join(script_dir, "..", "LLM Model", "Qwen3.5-9B-GGUF"),
        # Relative to current working directory
        os.path.join(os.getcwd(), "LLM Model", "Qwen3.5-9B-GGUF"),
        os.path.join(os.getcwd(), "template-offline", "LLM Model", "Qwen3.5-9B-GGUF")
    ]
    
    for base in candidates:
        model = os.path.join(base, "Qwen3.5-9B-Q4_K_M.gguf")
        mmproj = os.path.join(base, "mmproj-Qwen3.5-9B-BF16.gguf")
        if os.path.exists(model):
            return os.path.abspath(model), (os.path.abspath(mmproj) if os.path.exists(mmproj) else None)
            
    return None, None

GLOBAL_RAG_CONTEXT = ""

def extract_rag_context(rag_documents: list, user_query: str = "", max_total_chars: int = 3500) -> str:
    """
    Extracts relevant contextual knowledge from user-provided PDF documents using pypdf.
    If query keywords match pages/paragraphs, prioritizes those excerpts; otherwise takes
    representative excerpts from the documents up to max_total_chars.
    """
    if not rag_documents:
        return ""

    try:
        from pypdf import PdfReader
    except ImportError:
        try:
            from pypdf2 import PdfReader
        except ImportError:
            sys.stderr.write("Aviso RAG: biblioteca pypdf no encontrada en el entorno de Python.\n")
            return ""

    collected_chunks = []
    query_terms = [t.lower() for t in re.findall(r"\w{4,}", user_query)]

    for doc_path in rag_documents:
        if not doc_path or not os.path.exists(doc_path):
            continue
        try:
            reader = PdfReader(doc_path)
            doc_name = os.path.basename(doc_path)
            doc_text_parts = []
            matching_parts = []

            pages_to_read = min(len(reader.pages), 40)
            for page_idx in range(pages_to_read):
                page = reader.pages[page_idx]
                text = page.extract_text()
                if not text:
                    continue
                clean_text = re.sub(r"\s+", " ", text).strip()
                if not clean_text:
                    continue
                text_lower = clean_text.lower()
                hits = sum(1 for qt in query_terms if qt in text_lower)
                if hits > 0:
                    matching_parts.append((hits, clean_text[:1000]))
                else:
                    if len(doc_text_parts) < 3:
                        doc_text_parts.append(clean_text[:600])

            matching_parts.sort(key=lambda x: x[0], reverse=True)
            chosen = [p[1] for p in matching_parts[:3]]
            if not chosen and doc_text_parts:
                chosen = doc_text_parts[:2]

            if chosen:
                combined_doc = " | ".join(chosen)
                collected_chunks.append(f"[{doc_name}]: {combined_doc}")
        except Exception as ex_pdf:
            sys.stderr.write(f"Aviso RAG al procesar '{doc_path}': {ex_pdf}\n")

    if not collected_chunks:
        return ""

    rag_text = "\n".join(collected_chunks)
    if len(rag_text) > max_total_chars:
        rag_text = rag_text[:max_total_chars] + "... [truncado]"

    return rag_text.strip()

SAFETY_PROHIBITED_TERMS = [
    "nude", "nudity", "naked", "sex", "sexual", "porn", "erotic", "desnudo", "desnuda",
    "weapon", "weapons", "gun", "guns", "pistol", "rifle", "firearm", "sword", "knife", "explosive", "bomb", "arma", "armas",
    "drug", "drugs", "cocaine", "heroin", "marijuana", "cannabis", "syringe", "narcotic", "droga", "drogas"
]

def search_freerangestock_images(query: str, theme: str = "", limit: int = 6):
    """
    Searches Freerange Stock (https://freerangestock.com/) API for royalty-free stock photos.
    Uses the direct v2-backend image search API.
    """
    import requests
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Content-Type": "application/json",
        "Accept": "*/*",
        "Origin": "https://freerangestock.com",
        "Referer": "https://freerangestock.com/",
    }
    results = []
    seen = set()

    core_query = re.sub(r'\(.*?\)', '', query).split(',')[0].strip()
    if not core_query:
        core_query = query.strip()

    if any(term in core_query.lower() for term in SAFETY_PROHIBITED_TERMS):
        return results

    search_terms = [core_query]
    words = core_query.split()
    if len(words) > 1 and len(words[0]) > 2:
        search_terms.append(words[0])

    for st in search_terms:
        if len(results) >= limit:
            break
        for filter_mode in ["popular-30", "all", "most-popular"]:
            if len(results) >= limit:
                break
            try:
                payload = {
                    "search": st,
                    "gallery_id": "all",
                    "page": 1,
                    "items": limit * 2,
                    "filter": filter_mode,
                    "type": "photos",
                    "licenseType": ""
                }
                r = requests.post("https://v2-backend.freerangestock.com/api/v1/image/search", json=payload, headers=headers, timeout=8)
                if r.status_code == 200:
                    items = r.json().get("result", [])
                    for item in items:
                        img_list = item.get("images", [])
                        if not img_list:
                            continue
                        img_url = img_list[0].get("image") or img_list[0].get("thumbnail")
                        if img_url and img_url not in seen:
                            clean_u = img_url.split("?")[0].lower()
                            if any(t in clean_u for t in SAFETY_PROHIBITED_TERMS):
                                continue
                            seen.add(img_url)
                            results.append({
                                "url": img_url,
                                "license": "Freerange Stock (Free License)",
                                "copyright_warning": False,
                                "title": item.get("title", "")
                            })
                            if len(results) >= limit:
                                break
            except Exception:
                pass
    return results

FOLDER_CONTEXT_ENGINES = {}

STOP_WORDS = {
    "el", "la", "los", "las", "de", "del", "un", "una", "unos", "unas", "y", "o", "en", "a",
    "the", "a", "an", "of", "and", "or", "in", "to", "for", "with", "on", "at"
}

def _clean_tokens(text: str) -> list:
    """Extracts clean lowercase alphanumeric tokens without accents, filtering non-essential stop words."""
    if not text:
        return []
    cleaned = text.lower().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ü", "u").replace("ñ", "n")
    raw_tokens = [t for t in re.split(r'[^a-z0-9]+', cleaned) if len(t) > 1]
    meaningful = [t for t in raw_tokens if t not in STOP_WORDS]
    return meaningful if meaningful else raw_tokens

class HierarchicalFolderContextEngine:
    """
    Understands the complete semantic and structural context of a local folder and all its child subfolders.
    Recognizes entity container folders (e.g. 'Paris/01.jpg'), hierarchical classifications (e.g. 'Animals/Felines/tiger.jpg'),
    and descriptive file names, scoring candidates so the optimal candidates are evaluated first without
    relying on arbitrary physical appearance order.
    """
    def __init__(self, root_path: str):
        self.root_path = os.path.abspath(root_path)
        self.root_name = os.path.basename(self.root_path) or self.root_path
        self.indexed_files = []
        self.total_dirs = 0
        self.total_files = 0
        self._index_directory()

    def _index_directory(self):
        SUPPORTED_EXTS = {".jpg", ".jpeg", ".png", ".webp"}
        dir_to_files = {}

        temp_entries = []
        for root, dirs, files in os.walk(self.root_path):
            self.total_dirs += 1
            rel_dir = os.path.relpath(root, self.root_path)
            norm_rel_dir = "" if rel_dir == "." else rel_dir.replace("\\", "/")

            # Extract hierarchy segments
            segments = [s for s in norm_rel_dir.split("/") if s]
            immediate_parent = segments[-1] if segments else self.root_name
            ancestor_segments = segments[:-1] if len(segments) > 1 else ([self.root_name] if segments else [])

            dir_files = []
            for fname in files:
                ext = os.path.splitext(fname)[1].lower()
                if ext not in SUPPORTED_EXTS:
                    continue
                full_path = os.path.join(root, fname).replace("\\", "/")
                if any(term in full_path.lower() for term in SAFETY_PROHIBITED_TERMS):
                    continue

                f_base = os.path.splitext(fname)[0]
                dir_files.append(f_base)
                rel_f_path = (norm_rel_dir + "/" + fname) if norm_rel_dir else fname

                temp_entries.append({
                    "full_path": full_path,
                    "rel_path": rel_f_path,
                    "filename": fname,
                    "file_base": f_base,
                    "norm_rel_dir": norm_rel_dir,
                    "immediate_parent": immediate_parent,
                    "ancestors": ancestor_segments,
                    "file_tokens": _clean_tokens(f_base),
                    "parent_tokens": _clean_tokens(immediate_parent),
                    "ancestor_tokens": list(set(_clean_tokens(" ".join(ancestor_segments)))),
                })

            dir_to_files[norm_rel_dir] = dir_files

        # Compute sibling cluster tokens and finalize
        for entry in temp_entries:
            siblings = dir_to_files.get(entry["norm_rel_dir"], [])
            sibling_tokens = []
            for sib in siblings[:15]:
                if sib != entry["file_base"]:
                    sibling_tokens.extend(_clean_tokens(sib))

            entry["sibling_tokens"] = list(set(sibling_tokens))
            entry["all_hierarchy_str"] = f"{' '.join(entry['ancestors'])} {entry['immediate_parent']} {entry['file_base']}".lower()
            self.indexed_files.append(entry)
            self.total_files += 1

    def match_and_rank_candidates(self, query: str, theme: str = "", limit: int = 12) -> list:
        """
        Evaluates the search query and theme in the context of the indexed folder hierarchy.
        Returns candidates strictly sorted by contextual relevance descending.
        """
        if not self.indexed_files:
            return []

        core = re.sub(r'\(.*?\)', '', query).split(',')[0].strip().lower()
        query_tokens = _clean_tokens(core)
        theme_tokens = _clean_tokens(theme)
        full_query_clean = " ".join(query_tokens)

        scored_candidates = []

        for f in self.indexed_files:
            score = 0.0

            parent_clean = " ".join(f["parent_tokens"])
            file_clean = " ".join(f["file_tokens"])
            all_path_clean = f["all_hierarchy_str"]

            # --- TIER 1: Immediate Folder Match (Entity Container Pattern) ---
            # e.g. User has photos in a folder named after the entity: "Paris", "Tigre", "Audi R8"
            if full_query_clean and full_query_clean == parent_clean:
                score += 85.0
            elif full_query_clean and (full_query_clean in parent_clean or parent_clean in full_query_clean):
                score += 72.0
            elif query_tokens and all(q_tok in f["parent_tokens"] for q_tok in query_tokens):
                score += 68.0
            else:
                p_matches = sum(1 for q_tok in query_tokens if q_tok in f["parent_tokens"])
                if p_matches > 0:
                    score += 35.0 * (p_matches / max(1, len(query_tokens)))

            # --- TIER 2: Direct File Name Match (Descriptive Filename Pattern) ---
            # e.g. User has a file named "paris.jpg" or "tigre_bengala.png"
            if full_query_clean and full_query_clean == file_clean:
                score += 85.0
            elif full_query_clean and full_query_clean in file_clean:
                score += 72.0
            elif query_tokens and all(q_tok in f["file_tokens"] for q_tok in query_tokens):
                score += 68.0
            else:
                f_matches = sum(1 for q_tok in query_tokens if q_tok in f["file_tokens"])
                if f_matches > 0:
                    score += 35.0 * (f_matches / max(1, len(query_tokens)))

            # Exact core phrase match anywhere in the relative hierarchy path
            if core and core in all_path_clean:
                score += 20.0

            # --- TIER 3: Cross-Hierarchy Compound Match (Taxonomy Synergy) ---
            # e.g. Folder is "Francia" and File is "torre_eiffel.jpg", Query is "Torre Eiffel Francia"
            folder_has_tok = any(q_tok in f["parent_tokens"] or q_tok in f["ancestor_tokens"] for q_tok in query_tokens)
            file_has_tok = any(q_tok in f["file_tokens"] for q_tok in query_tokens)
            if folder_has_tok and file_has_tok:
                score += 30.0  # Compound synergy bonus!

            # --- TIER 4: Ancestor Theme & Category Alignment ---
            # e.g. Ancestor folder matches game theme (e.g. "Animales", "Monumentos", "Ciencias")
            if theme_tokens:
                anc_theme_matches = sum(1 for th_tok in theme_tokens if th_tok in f["ancestor_tokens"] or th_tok in f["parent_tokens"])
                if anc_theme_matches > 0:
                    score += 15.0

            # --- TIER 5: Sibling Cluster Semantic Resonance ---
            if any(q_tok in f["sibling_tokens"] for q_tok in query_tokens):
                score += 12.0

            # Complete query coverage bonus across the whole path
            if query_tokens and all(q_tok in f["file_tokens"] or q_tok in f["parent_tokens"] or q_tok in f["ancestor_tokens"] for q_tok in query_tokens):
                score += 15.0

            if score > 0:
                scored_candidates.append({
                    "url": f["full_path"],
                    "license": f"Local Folder ({f['immediate_parent']})",
                    "copyright_warning": False,
                    "is_local": True,
                    "rel_path": f["rel_path"],
                    "folder_context": f["norm_rel_dir"] or f["immediate_parent"],
                    "file_context": f["file_base"],
                    "path_score": min(100.0, score)
                })

        # Sort strictly by path_score descending (highest probability candidate tested first)
        scored_candidates.sort(key=lambda x: x["path_score"], reverse=True)

        if scored_candidates:
            best = scored_candidates[0]
            sys.stderr.write(f"[Contexto Local] '{query}' -> Mejor coincidencia: '{best['rel_path']}' (Score: {best['path_score']:.0f}/100, Carpeta: '{best['folder_context']}')\n")

        return scored_candidates[:limit]

def search_local_folder_images(folder_path: str, query: str, theme: str = "", limit: int = 12, llm = None):
    """
    Searches a local user folder using hierarchical semantic context indexing.
    Understands entity container folders, category paths, and descriptive filenames,
    ranking candidates so the highest-relevance images are evaluated first without
    relying on arbitrary physical appearance order.
    """
    clean_folder = folder_path[7:] if folder_path.startswith("folder:") else folder_path
    clean_folder = os.path.expanduser(os.path.expandvars(clean_folder.strip()))
    if not os.path.exists(clean_folder) or not os.path.isdir(clean_folder):
        return []

    norm_key = os.path.abspath(clean_folder).lower()
    if norm_key not in FOLDER_CONTEXT_ENGINES:
        sys.stderr.write(f"Indexando contexto semántico de carpeta local: '{clean_folder}'...\n")
        engine = HierarchicalFolderContextEngine(clean_folder)
        FOLDER_CONTEXT_ENGINES[norm_key] = engine
        sys.stderr.write(f"Contexto indexado: {engine.total_dirs} carpetas, {engine.total_files} imágenes.\n")
    else:
        engine = FOLDER_CONTEXT_ENGINES[norm_key]

    return engine.match_and_rank_candidates(query=query, theme=theme, limit=limit)

def scrape_custom_url_images(target_url: str, query: str = "", limit: int = 6):
    """
    Scrapes or resolves image candidate URLs from a user-provided custom web URL.
    Supports direct images as well as web galleries.
    """
    import requests
    from bs4 import BeautifulSoup
    import urllib.parse
    results = []
    seen = set()

    clean_url = target_url.strip()
    if not clean_url:
        return results

    # Direct image URL
    low = clean_url.lower().split("?")[0]
    if low.endswith((".jpg", ".jpeg", ".png", ".webp")):
        return [{
            "url": clean_url,
            "license": "Custom Web Image",
            "copyright_warning": False
        }]

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    }
    try:
        r = requests.get(clean_url, headers=headers, timeout=8)
        if r.status_code != 200:
            return results
        soup = BeautifulSoup(r.text, "html.parser")

        candidate_urls = []
        for meta in soup.find_all("meta"):
            if meta.get("property") in ["og:image", "twitter:image"] and meta.get("content"):
                candidate_urls.append(meta.get("content"))

        for img in soup.find_all("img"):
            src = img.get("src") or img.get("data-src") or ""
            if src:
                candidate_urls.append(urllib.parse.urljoin(clean_url, src))

        for img_url in candidate_urls:
            u_clean = img_url.split("?")[0].lower()
            if not u_clean.endswith((".jpg", ".jpeg", ".png", ".webp")):
                continue
            if any(ign in u_clean for ign in ["logo", "icon", "avatar", "badge", "button", "arrow", "spinner", "advert"]):
                continue
            if any(term in u_clean for term in SAFETY_PROHIBITED_TERMS):
                continue
            if img_url in seen:
                continue

            seen.add(img_url)
            results.append({
                "url": img_url,
                "license": f"External Web ({urllib.parse.urlparse(clean_url).netloc})",
                "copyright_warning": False
            })
            if len(results) >= limit:
                break
    except Exception:
        pass

    return results

def search_wikipedia_images(query: str, theme: str = "", limit: int = 6):
    """
    Searches Wikipedia (ES & EN) PageImages API for official authoritative lead infobox photos.
    """
    import requests
    headers = {"User-Agent": "GameShowCenter/1.0 (https://gameshowcenter.org; contact@gameshowcenter.org)"}
    results = []
    seen = set()

    core_query = re.sub(r'\(.*?\)', '', query).split(',')[0].strip()
    if not core_query:
        core_query = query.strip()

    if any(term in core_query.lower() for term in SAFETY_PROHIBITED_TERMS):
        return results

    for lang in ["es", "en"]:
        for search_term in [query, core_query]:
            if not search_term:
                continue
            try:
                r_wiki = requests.get(f"https://{lang}.wikipedia.org/w/api.php", params={
                    "action": "query",
                    "titles": search_term,
                    "prop": "pageimages|description",
                    "piprop": "thumbnail|original",
                    "pithumbsize": 960,
                    "format": "json",
                    "redirects": 1
                }, headers=headers, timeout=6)
                if r_wiki.status_code == 200:
                    pages = r_wiki.json().get("query", {}).get("pages", {})
                    for pid, p in pages.items():
                        if pid == "-1":
                            continue
                        thumb = p.get("thumbnail", {}).get("source", "")
                        orig = p.get("original", {}).get("source", "")
                        best_url = thumb if thumb else orig
                        if best_url and best_url not in seen:
                            clean_u = best_url.split("?")[0].lower()
                            if any(t in clean_u for t in SAFETY_PROHIBITED_TERMS):
                                continue
                            if not clean_u.endswith(".svg"):
                                seen.add(best_url)
                                results.append({
                                    "url": best_url,
                                    "license": f"Wikipedia ({lang.upper()})",
                                    "copyright_warning": False
                                })
                                if len(results) >= limit:
                                    return results
            except Exception:
                pass
    return results

def search_wikimedia_images(query: str, theme: str = "", limit: int = 6):
    """
    Searches Wikimedia Commons API for direct high-quality bitmap images.
    """
    import requests
    headers = {"User-Agent": "GameShowCenter/1.0 (https://gameshowcenter.org; contact@gameshowcenter.org)"}
    results = []
    seen = set()

    core_query = re.sub(r'\(.*?\)', '', query).split(',')[0].strip()
    if not core_query:
        core_query = query.strip()

    if any(term in core_query.lower() for term in SAFETY_PROHIBITED_TERMS):
        return results

    try:
        safety_negatives = "-nude -naked -sex -porn -erotic -weapon -gun -rifle -pistol -bomb -drug -drugs -cocaine -marijuana"
        search_query = f'"{core_query}" filetype:bitmap {safety_negatives} -flower -clematis -diagram -chart -stamp -coin -signature'
        r = requests.get("https://commons.wikimedia.org/w/api.php", params={
            "action": "query",
            "list": "search",
            "srsearch": search_query,
            "srnamespace": "6",
            "srlimit": limit * 2,
            "format": "json"
        }, headers=headers, timeout=6)

        if r.status_code == 200:
            search_items = r.json().get("query", {}).get("search", [])
            unwanted_words = ["flower", "clematis", "diagram", "stamp", "coin", "signature", "blank", "icon", "chart", "map"] + SAFETY_PROHIBITED_TERMS
            titles = [
                item["title"] for item in search_items
                if not any(w in item["title"].lower() for w in unwanted_words)
            ]

            if titles:
                r_info = requests.get("https://commons.wikimedia.org/w/api.php", params={
                    "action": "query",
                    "titles": "|".join(titles[:14]),
                    "prop": "imageinfo",
                    "iiprop": "url|extmetadata",
                    "iiurlwidth": 960,
                    "format": "json"
                }, headers=headers, timeout=7)

                if r_info.status_code == 200:
                    pages = r_info.json().get("query", {}).get("pages", {})
                    for pid, p in pages.items():
                        info_list = p.get("imageinfo", [])
                        if not info_list:
                            continue
                        info = info_list[0]
                        raw_url = info.get("thumburl") or info.get("url", "")
                        if not raw_url or raw_url in seen:
                            continue

                        clean_url = raw_url.split("?")[0].lower()
                        if any(t in clean_url for t in SAFETY_PROHIBITED_TERMS):
                            continue
                        if clean_url.endswith(".svg"):
                            continue

                        seen.add(raw_url)
                        meta = info.get("extmetadata", {})
                        lic = meta.get("LicenseShortName", {}).get("value", "")
                        is_pd = any(term in lic.lower() for term in ["public domain", "cc0", "pd", "no restrictions"])

                        results.append({
                            "url": raw_url,
                            "license": lic if lic else "Wikimedia Commons",
                            "copyright_warning": not is_pd
                        })
                        if len(results) >= limit:
                            break
    except Exception:
        pass

    return results

def search_openverse_images(query: str, limit: int = 6):
    """
    Searches Openverse API (https://api.openverse.org/v1/images/) for Creative Commons images.
    """
    import requests
    headers = {"User-Agent": "GameShowCenter/1.0 (contact@gameshowcenter.org)"}
    results = []
    seen = set()
    core_query = re.sub(r'\(.*?\)', '', query).split(',')[0].strip()
    if not core_query or any(term in core_query.lower() for term in SAFETY_PROHIBITED_TERMS):
        return results
    try:
        url = "https://api.openverse.org/v1/images/"
        params = {
            "q": core_query,
            "page_size": min(20, limit * 2)
        }
        r = requests.get(url, params=params, headers=headers, timeout=7)
        if r.status_code == 200:
            data = r.json()
            items = data.get("results", [])
            for it in items:
                img_url = it.get("url")
                if not img_url or img_url in seen:
                    continue
                clean_u = img_url.split("?")[0].lower()
                if any(t in clean_u for t in SAFETY_PROHIBITED_TERMS):
                    continue
                if not clean_u.endswith((".jpg", ".jpeg", ".png", ".webp")):
                    continue
                seen.add(img_url)
                results.append({
                    "url": img_url,
                    "license": f"Openverse ({it.get('license', 'CC')})",
                    "copyright_warning": False,
                    "title": it.get("title", "")
                })
                if len(results) >= limit:
                    break
    except Exception:
        pass
    return results

def search_nasa_images(query: str, limit: int = 6):
    """
    Searches NASA Images API (https://images-api.nasa.gov/search) for public domain NASA media.
    """
    import requests
    results = []
    seen = set()
    core_query = re.sub(r'\(.*?\)', '', query).split(',')[0].strip()
    if not core_query or any(term in core_query.lower() for term in SAFETY_PROHIBITED_TERMS):
        return results
    try:
        url = "https://images-api.nasa.gov/search"
        params = {
            "q": core_query,
            "media_type": "image"
        }
        r = requests.get(url, params=params, timeout=7)
        if r.status_code == 200:
            data = r.json()
            items = data.get("collection", {}).get("items", [])
            for it in items:
                links = it.get("links", [])
                title = ""
                d_list = it.get("data", [])
                if d_list:
                    title = d_list[0].get("title", "")
                for lk in links:
                    href = lk.get("href", "")
                    if href and href not in seen:
                        clean_u = href.split("?")[0].lower()
                        if any(t in clean_u for t in SAFETY_PROHIBITED_TERMS):
                            continue
                        if clean_u.endswith((".jpg", ".jpeg", ".png", ".webp")):
                            seen.add(href)
                            results.append({
                                "url": href,
                                "license": "NASA Public Domain",
                                "copyright_warning": False,
                                "title": title
                            })
                            break
                if len(results) >= limit:
                    break
    except Exception:
        pass
    return results

def fetch_candidate_images(game_name: str, query: str, theme: str = "", limit: int = 12, image_sources: list = None, llm = None):
    """
    Searches across configured image sources (5 fixed internet repos, school URLs, local folders)
    for the target query and its variations.
    """
    if image_sources is None or not image_sources:
        image_sources = ["wikimedia", "wikipedia", "freerangestock", "openverse", "nasa"]

    results = []
    seen = set()

    core_query = re.sub(r'\(.*?\)', '', query).split(',')[0].strip()
    if not core_query:
        core_query = query.strip()

    search_queries = [core_query]
    if query.strip() != core_query:
        search_queries.append(query.strip())

    # Strip leading articles (e.g. "El Coliseo" -> "Coliseo") for broader repository hits
    clean_article = re.sub(r'^(el|la|los|las|un|una|the|a|an)\s+', '', core_query, flags=re.IGNORECASE).strip()
    if clean_article and clean_article.lower() != core_query.lower() and len(clean_article) > 2:
        search_queries.append(clean_article)

    if theme and theme.strip() and theme.strip().lower() not in core_query.lower():
        search_queries.append(f"{core_query} {theme.strip()}")

    per_source_limit = max(4, limit // max(1, len(image_sources)) + 3)

    for src in image_sources:
        src_str = str(src).strip()
        if not src_str:
            continue

        src_candidates = []

        # 1. Local Folder lookup (hierarchical tree context analysis)
        if src_str.startswith("folder:") or os.path.isdir(src_str) or (len(src_str) >= 3 and src_str[1:3] in (":\\", ":/")) or src_str.startswith("\\\\"):
            for q in search_queries[:2]:
                folder_res = search_local_folder_images(src_str, q, theme=theme, limit=per_source_limit, llm=llm)
                for item in folder_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        # 2. Openverse
        elif "openverse" in src_str.lower():
            for q in search_queries[:2]:
                ov_res = search_openverse_images(q, limit=per_source_limit)
                for item in ov_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        # 3. NASA Images
        elif "nasa" in src_str.lower():
            for q in search_queries[:2]:
                nasa_res = search_nasa_images(q, limit=per_source_limit)
                for item in nasa_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        # 4. Freerange Stock
        elif "freerange" in src_str.lower():
            for q in search_queries:
                fr_res = search_freerangestock_images(q, theme=theme, limit=per_source_limit)
                for item in fr_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        # 5. Wikimedia Commons
        elif "wikimedia" in src_str.lower():
            for q in search_queries:
                wiki_res = search_wikimedia_images(q, theme=theme, limit=per_source_limit)
                for item in wiki_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        # 6. Wikipedia PageImages
        elif "wikipedia" in src_str.lower():
            for q in search_queries:
                wp_res = search_wikipedia_images(q, theme=theme, limit=per_source_limit)
                for item in wp_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        # 7. School / Custom Web URL
        elif (src_str.startswith("http://") or src_str.startswith("https://")):
            for q in search_queries[:2]:
                url_res = scrape_custom_url_images(src_str, query=q, limit=per_source_limit)
                for item in url_res:
                    if item["url"] not in seen:
                        seen.add(item["url"])
                        src_candidates.append(item)
                if len(src_candidates) >= per_source_limit:
                    break

        results.extend(src_candidates)

    return results


def _do_verify_image_with_vision(llm, img_url: str, subject: str, theme: str = "", custom_prompt_template: str = "", cand_meta: dict = None, strictness: int = 5):
    """
    Downloads image in memory, resizes thumbnail, and uses multimodal Qwen to strictly verify
    and score that the image depicts the requested subject AND matches the user's theme.
    Understands local folder/file hierarchy context and applies the user's strictness threshold (1 to 10).
    Returns (is_valid: bool, raw_bytes: bytes | None, score: float)
    """
    import requests
    import base64
    from io import BytesIO
    from PIL import Image, ImageFile

    ImageFile.LOAD_TRUNCATED_IMAGES = True

    # Filter out SVGs or non-image URLs
    clean_url = img_url.split("?")[0].lower()
    if clean_url.endswith(".svg"):
        return False, None, 0.0

    raw_bytes = bytearray()
    if os.path.exists(img_url):
        try:
            with open(img_url, "rb") as f:
                raw_bytes = bytearray(f.read())
        except Exception:
            return False, None, 0.0
    else:
        headers = {"User-Agent": "GameShowCenter/1.0 (https://gameshowcenter.org; contact@gameshowcenter.org)"}
        try:
            r = requests.get(img_url, headers=headers, timeout=(5, 15), stream=True)
            if r.status_code != 200:
                return False, None, 0.0
            content_type = r.headers.get("Content-Type", "").lower()
            if content_type and not any(t in content_type for t in ["image", "octet-stream"]):
                return False, None, 0.0
        except Exception:
            return False, None, 0.0

        for chunk in r.iter_content(chunk_size=65536):
            raw_bytes.extend(chunk)
            if len(raw_bytes) > 15 * 1024 * 1024:  # 15 MB cap
                break

    if len(raw_bytes) < 1000:
        return False, None, 0.0

    # Local folder semantic hierarchy bonus (up to 20 points)
    local_bonus = 0.0
    local_info = ""
    if cand_meta and cand_meta.get("is_local"):
        p_score = cand_meta.get("path_score", 0)
        local_bonus = (p_score / 100.0) * 20.0
        f_ctx = cand_meta.get("folder_context", "")
        fb = cand_meta.get("file_context", "")
        if f_ctx or fb:
            local_info = f"\nLOCAL FOLDER HIERARCHY CONTEXT:\n- Relative Child Folder: '{f_ctx}'\n- File Name: '{fb}'\n- Semantic Path Score: {p_score}/100\n"

    # Check image integrity and calculate technical quality score
    try:
        orig_img = Image.open(BytesIO(raw_bytes))
        w, h = orig_img.size
        if w < 180 or h < 180 or (w / max(1, h) > 3.5) or (h / max(1, w) > 3.5):
            return False, None, 0.0

        # Technical Score (up to 35 points):
        # 1. Resolution score (up to 20 pts)
        pixels = w * h
        res_score = min(20.0, (pixels / (1280.0 * 720.0)) * 12.0)
        # 2. Aspect ratio score (up to 10 pts - optimal landscape or square for cards)
        ratio = w / max(1.0, float(h))
        if 1.2 <= ratio <= 1.85:
            ratio_score = 10.0
        elif 0.9 <= ratio <= 1.15:
            ratio_score = 7.0
        else:
            ratio_score = 3.0
        # 3. Byte quality score (up to 5 pts)
        size_score = min(5.0, (len(raw_bytes) / 150000.0) * 5.0)

        tech_score = res_score + ratio_score + size_score

        img = orig_img.copy()
        img.draft("RGB", (384, 384))
        img = img.convert("RGB")
        img.thumbnail((384, 384))
    except Exception:
        return False, None, 0.0

    # If LLM or multimodal handler is not active:
    if llm is None or not hasattr(llm, "chat_handler") or llm.chat_handler is None:
        if strictness == 1:
            return True, bytes(raw_bytes), 25.0 + local_bonus
        return False, None, 0.0

    buf = BytesIO()
    img.save(buf, format="JPEG", quality=80)
    b64_img = f"data:image/jpeg;base64,{base64.b64encode(buf.getvalue()).decode('utf-8')}"

    context_theme = theme.strip() if theme else "General Culture"
    rag_clause = f"\nADDITIONAL REFERENCE KNOWLEDGE FROM ATTACHED PDF DOCUMENTS:\n{GLOBAL_RAG_CONTEXT[:1200]}\n" if GLOBAL_RAG_CONTEXT else ""
    if custom_prompt_template and custom_prompt_template.strip():
        try:
            prompt_text = custom_prompt_template.format(subject=subject, context_theme=context_theme, theme=context_theme)
        except Exception:
            prompt_text = custom_prompt_template.replace("{subject}", subject).replace("{context_theme}", context_theme).replace("{theme}", context_theme)
        if local_info:
            prompt_text += f"\n{local_info}"
        if rag_clause:
            prompt_text += f"\n{rag_clause}"
    else:
        prompt_text = (
            f"You are an expert visual validation and quality judge for a premium game show.\n"
            f"Target entity: '{subject}'.\n"
            f"General theme: '{context_theme}'.\n"
            f"{local_info}\n"
            f"{rag_clause}\n"
            f"CRITICAL SAFETY MANDATE (SCORE: 0 IMMEDIATELY):\n"
            f"- Reject immediately with SCORE: 0 if the image contains ANY nudity, naked bodies, sexual content, weapons, violence, or illicit drugs.\n\n"
            f"RELEVANCE & VISUAL QUALITY:\n"
            f"- If '{subject}' is unrecognizable, absent, or depicts something totally unrelated, answer SCORE: 0.\n"
            f"- If '{subject}' is clearly shown or supported by the local folder hierarchy context, rate its visual quality, clarity, aesthetic appeal, and relevance on a scale from 1 to 10 (10 = perfect, sharp, beautiful representation).\n\n"
            f"Answer strictly in this format:\n"
            f"SCORE: <0-10>\n"
            f"VERDICT: <YES or NO>"
        )
    try:
        res = llm.create_chat_completion(
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": b64_img}},
                        {"type": "text", "text": prompt_text}
                    ]
                }
            ],
            max_tokens=64,
            temperature=0.0
        )
        answer = res["choices"][0]["message"]["content"].strip()
        clean_answer = answer.split("</think>")[-1].strip().upper()

        # Parse vision score (0 to 10)
        vision_score = 0.0
        score_match = re.search(r'SCORE:\s*(\d+(?:\.\d+)?)', clean_answer)
        if score_match:
            try:
                vision_score = float(score_match.group(1))
            except ValueError:
                vision_score = 0.0
        elif "YES" in clean_answer and "NO" not in clean_answer:
            vision_score = 7.5

        # Strictness handling on base 10 scale (1 to 10):
        # Strictness 1: Only NSFW/safety failures (SCORE: 0) rejected
        if strictness == 1:
            if vision_score == 0.0:
                return False, None, 0.0
            total_score = max(15.0, (min(10.0, vision_score) * 6.5) + tech_score + local_bonus)
            return True, bytes(raw_bytes), min(100.0, total_score)

        # Strictness 2..10:
        # Scale minimum vision score required (at 2 -> 2.5, at 5 -> 4.2, at 10 -> 7.0)
        min_vision_score = 2.0 + (strictness - 1) * 0.55
        if vision_score < min_vision_score or ("NO" in clean_answer and vision_score < (min_vision_score + 1.2)):
            return False, None, 0.0

        total_score = (min(10.0, vision_score) * 6.5) + tech_score + local_bonus
        return True, bytes(raw_bytes), min(100.0, total_score)
    except Exception as ex:
        sys.stderr.write(f"Vision verification exception for '{subject}': {ex}\n")
        return False, None, 0.0

def verify_image_with_vision(llm, img_url: str, subject: str, theme: str = "", custom_prompt_template: str = "", timeout_sec: float = 40.0, cand_meta: dict = None, strictness: int = 5):
    """
    Downloads and verifies an image with multimodal Qwen, enforcing strict topic matching, quality scoring, and timeout.
    Returns (is_valid: bool, raw_bytes: bytes | None, score: float).
    """
    import concurrent.futures
    executor = concurrent.futures.ThreadPoolExecutor(max_workers=1)
    future = executor.submit(_do_verify_image_with_vision, llm, img_url, subject, theme, custom_prompt_template, cand_meta, strictness)
    try:
        result = future.result(timeout=timeout_sec)
        executor.shutdown(wait=False, cancel_futures=True)
        if isinstance(result, tuple) and len(result) == 3:
            return result
        elif isinstance(result, tuple) and len(result) == 2:
            return result[0], result[1], (80.0 if result[0] else 0.0)
        return bool(result), None, 0.0
    except concurrent.futures.TimeoutError:
        sys.stderr.write(f"Timeout de {timeout_sec}s procesando imagen {img_url}. Reintentando con otro URL...\n")
        executor.shutdown(wait=False, cancel_futures=True)
        return False, None, 0.0
    except Exception as e:
        sys.stderr.write(f"Conflicto procesando imagen {img_url} ({e}). Reintentando con otro URL...\n")
        executor.shutdown(wait=False, cancel_futures=True)
        return False, None, 0.0

def resolve_game_folder_name(game_name: str) -> str:
    """
    Resolves the canonical folder name for the given game, handling differences
    like case or spaces vs underscores (e.g. 'Snap_Solve' vs 'Snap Solve').
    """
    script_dir = os.path.dirname(os.path.abspath(__file__))
    search_roots = [
        os.path.join(script_dir, "..", "games"),
        os.path.join(os.getcwd(), "games"),
        os.path.join(os.getcwd(), "template-offline", "games")
    ]
    norm_target = re.sub(r'[^a-zA-Z0-9]', '', game_name).lower()
    for root in search_roots:
        if os.path.exists(root) and os.path.isdir(root):
            for folder in os.listdir(root):
                if os.path.isdir(os.path.join(root, folder)):
                    if re.sub(r'[^a-zA-Z0-9]', '', folder).lower() == norm_target:
                        return folder
    return game_name

def get_game_images_dir(game_name: str):
    """
    Finds or creates local images directory for the given game.
    Returns (abs_dir_path, relative_prefix)
    """
    real_name = resolve_game_folder_name(game_name)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        os.path.join(script_dir, "..", "games", real_name, "images"),
        os.path.join(os.getcwd(), "games", real_name, "images"),
        os.path.join(os.getcwd(), "template-offline", "games", real_name, "images")
    ]
    for c in candidates:
        parent = os.path.dirname(c)
        if os.path.exists(parent) and os.path.isdir(parent):
            os.makedirs(c, exist_ok=True)
            return os.path.abspath(c), f"games/{real_name}/images"

    fallback = os.path.join(script_dir, "..", "games", real_name, "images")
    os.makedirs(fallback, exist_ok=True)
    return os.path.abspath(fallback), f"games/{real_name}/images"

def save_verified_image(raw_bytes: bytes, game_name: str, subject: str, index: int = 1) -> str:
    """
    Saves image bytes locally to games/<game_name>/images/<filename>.jpg.
    Returns the relative path 'games/<game_name>/images/<filename>.jpg'.
    """
    if not raw_bytes or len(raw_bytes) < 500:
        return ""

    real_name = resolve_game_folder_name(game_name)
    img_dir, rel_prefix = get_game_images_dir(real_name)
    clean_subj = re.sub(r'[^a-zA-Z0-9_\-]', '_', subject.lower().strip())
    clean_subj = re.sub(r'_+', '_', clean_subj)[:25].strip('_')
    if not clean_subj:
        clean_subj = "item"

    timestamp = int(time.time() * 1000) % 100000000
    filename = f"ai_{clean_subj}_{index}_{timestamp}.jpg"
    full_path = os.path.join(img_dir, filename)

    try:
        from PIL import Image
        from io import BytesIO
        img = Image.open(BytesIO(raw_bytes))
        img = img.convert("RGB")
        img.save(full_path, "JPEG", quality=85, optimize=True)

        # Mirror copy to any sibling directories (e.g. root games/ vs template-offline/games/)
        script_dir = os.path.dirname(os.path.abspath(__file__))
        mirror_candidates = [
            os.path.join(script_dir, "..", "games", real_name, "images", filename),
            os.path.join(os.getcwd(), "games", real_name, "images", filename),
            os.path.join(os.getcwd(), "template-offline", "games", real_name, "images", filename),
            os.path.join(os.getcwd(), "template-offline", "release", "GameShowCenter", "games", real_name, "images", filename),
            os.path.join(script_dir, "..", "release", "GameShowCenter", "games", real_name, "images", filename)
        ]
        import shutil
        for m in mirror_candidates:
            abs_m = os.path.abspath(m)
            if abs_m != os.path.abspath(full_path):
                try:
                    os.makedirs(os.path.dirname(abs_m), exist_ok=True)
                    shutil.copy2(full_path, abs_m)
                except Exception:
                    pass

        return f"{rel_prefix}/{filename}"
    except Exception as e:
        sys.stderr.write(f"Error saving image locally ({e}), trying direct write\n")
        try:
            with open(full_path, "wb") as f:
                f.write(raw_bytes)
            return f"{rel_prefix}/{filename}"
        except Exception as e2:
            sys.stderr.write(f"Direct write failed ({e2})\n")
            return ""

def compute_image_dhash(img, hash_size=8):
    """Computes a 64-bit difference hash (dHash) for fast visual similarity checks."""
    try:
        from PIL import Image
        resized = img.convert("L").resize((hash_size + 1, hash_size), Image.Resampling.LANCZOS)
        pixels = list(resized.getdata())
        val = 0
        for row in range(hash_size):
            for col in range(hash_size):
                p_left = pixels[row * (hash_size + 1) + col]
                p_right = pixels[row * (hash_size + 1) + col + 1]
                val = (val << 1) | (1 if p_left > p_right else 0)
        return val
    except Exception:
        return 0

def is_image_too_similar(img, existing_hashes, threshold=12):
    """
    Checks if an image is visually too similar or a duplicate angle compared to existing hashes.
    Returns (is_similar: bool, hash_or_distance: int).
    """
    if not existing_hashes:
        return False, compute_image_dhash(img)
    cand_hash = compute_image_dhash(img)
    for eh in existing_hashes:
        dist = bin(cand_hash ^ eh).count('1')
        if dist <= threshold:
            return True, dist
    return False, cand_hash

def purge_unused_images(game_name: str, used_relative_paths: list):
    """
    Purges all orphan generated AI images (ai_*.jpg/png) from games/<game_name>/images
    that are not in used_relative_paths. Protects default game assets.
    """
    real_name = resolve_game_folder_name(game_name)
    used_filenames = {os.path.basename(p).lower() for p in used_relative_paths if p}
    script_dir = os.path.dirname(os.path.abspath(__file__))
    dirs_to_check = [
        os.path.join(script_dir, "..", "games", real_name, "images"),
        os.path.join(os.getcwd(), "games", real_name, "images"),
        os.path.join(os.getcwd(), "template-offline", "games", real_name, "images")
    ]
    seen_dirs = set()
    for d in dirs_to_check:
        abs_d = os.path.abspath(d)
        if abs_d in seen_dirs or not os.path.exists(abs_d) or not os.path.isdir(abs_d):
            continue
        seen_dirs.add(abs_d)
        try:
            for fname in os.listdir(abs_d):
                lower_name = fname.lower()
                # Only delete generated AI images (starting with ai_) that are not currently used
                if lower_name.startswith("ai_") and (lower_name.endswith(".jpg") or lower_name.endswith(".png") or lower_name.endswith(".jpeg")):
                    if lower_name not in used_filenames:
                        try:
                            os.remove(os.path.join(abs_d, fname))
                        except Exception:
                            pass
        except Exception:
            pass

def find_game_ai_config(game_name: str) -> dict:
    """
    Locates and parses the ai_prompt.json configuration for the specified game.
    Searches template-offline/games/<game_name>/ai_prompt.json and games/<game_name>/ai_prompt.json.
    Matches game folder names flexibly (case-insensitive, underscores to spaces).
    """
    script_dir = os.path.dirname(os.path.abspath(__file__))
    search_roots = [
        os.path.join(script_dir, "..", "games"),
        os.path.join(os.getcwd(), "template-offline", "games"),
        os.path.join(os.getcwd(), "games")
    ]
    norm_target = game_name.lower().replace("_", " ").replace("-", " ").strip()

    for root in search_roots:
        if not os.path.exists(root) or not os.path.isdir(root):
            continue
        # 1. Exact match
        direct_path = os.path.join(root, game_name, "ai_prompt.json")
        if os.path.exists(direct_path):
            try:
                with open(direct_path, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        # 2. Case-insensitive / normalized match
        for folder in os.listdir(root):
            norm_folder = folder.lower().replace("_", " ").replace("-", " ").strip()
            if norm_folder == norm_target:
                cfg_path = os.path.join(root, folder, "ai_prompt.json")
                if os.path.exists(cfg_path):
                    try:
                        with open(cfg_path, "r", encoding="utf-8") as f:
                            return json.load(f)
                    except Exception:
                        pass

    return None

def repair_json_string(text: str) -> str:
    """Attempts to clean and repair common LLM JSON syntax errors."""
    if not text:
        return ""
    cleaned = text.strip()

    # Remove thinking tags if present (<think>...</think>)
    cleaned = re.sub(r"<think>[\s\S]*?</think>", "", cleaned, flags=re.IGNORECASE).strip()

    # Remove markdown code blocks if wrapped
    m_code = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", cleaned)
    if m_code:
        cleaned = m_code.group(1).strip()

    # Normalize typographical/smart quotes to standard ASCII quotes
    cleaned = cleaned.replace("“", "\"").replace("”", "\"").replace("’", "'").replace("‘", "'")

    # Remove trailing commas before closing brackets/braces (e.g., [1, 2, ] or {"a": 1, })
    cleaned = re.sub(r",\s*(\]|})", r"\1", cleaned)

    # If array is truncated (starts with [ but has no closing ])
    if cleaned.startswith("[") and not cleaned.endswith("]"):
        # Strip trailing incomplete element/comma and close bracket
        cut = re.sub(r",\s*[^,}\]]*$", "", cleaned)
        cleaned = cut.rstrip() + "]"

    # If object is truncated (starts with { but has no closing })
    if cleaned.startswith("{") and not cleaned.endswith("}"):
        cut = re.sub(r",\s*[^,}\]]*$", "", cleaned)
        cleaned = cut.rstrip() + "}"

    return cleaned

def extract_and_repair_json(text: str):
    """
    Extracts and parses a JSON array or object from LLM output.
    Applies aggressive progressive repair strategies and emergency regex entity extraction.
    """
    if not text or not text.strip():
        raise ValueError("Salida del modelo vacía.")

    raw = text.strip()

    # 1. Direct parse attempt
    try:
        return json.loads(raw)
    except Exception:
        pass

    # 2. Parse after smart cleaning & trailing comma / truncation repair
    repaired = repair_json_string(raw)
    try:
        return json.loads(repaired)
    except Exception:
        pass

    # 3. Find outermost brackets [ ... ]
    m_arr = re.search(r"(\[[\s\S]*\])", raw)
    if m_arr:
        try:
            return json.loads(m_arr.group(1))
        except Exception:
            try:
                return json.loads(repair_json_string(m_arr.group(1)))
            except Exception:
                pass

    # 4. Find outermost braces { ... }
    m_obj = re.search(r"(\{[\s\S]*\})", raw)
    if m_obj:
        try:
            return json.loads(m_obj.group(1))
        except Exception:
            try:
                return json.loads(repair_json_string(m_obj.group(1)))
            except Exception:
                pass

    # 5. Emergency regex recovery for arrays of objects (e.g. [{"question":..., "answer":...}])
    obj_matches = re.findall(r"\{[^{}]*\}", raw)
    if obj_matches:
        recovered_objs = []
        for om in obj_matches:
            try:
                item = json.loads(repair_json_string(om))
                if isinstance(item, dict):
                    recovered_objs.append(item)
            except Exception:
                pass
        if recovered_objs:
            return recovered_objs

    # 6. Emergency regex recovery for arrays of strings (e.g. ["word1", "word2"])
    str_matches = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', raw)
    if str_matches and len(str_matches) >= 2:
        valid_strs = [s.strip() for s in str_matches if s.strip() and not s.strip().lower().startswith("error")]
        if valid_strs:
            return valid_strs

    raise ValueError(f"No se pudo extraer una estructura JSON válida de la respuesta: {raw[:180]}")

def extract_json_block(text: str):
    """Backwards-compatible wrapper calling extract_and_repair_json."""
    return extract_and_repair_json(text)

def generate_text_llm(llm, system_prompt: str, user_prompt: str, max_tokens: int = 500, stream_callback=None):
    """
    Calls the LLM using assistant prefill to generate JSON.
    If generation fails to produce valid JSON, it automatically retries up to 3 times
    without reloading the model, analyzing the error and adapting decoding parameters
    and instructions on each attempt so no consumed time is lost.
    """
    MAX_ATTEMPTS = 3
    last_error = None
    cur_tokens = max_tokens
    cur_temp = 0.3
    cur_sys_prompt = system_prompt
    cur_user_prompt = user_prompt

    for attempt in range(1, MAX_ATTEMPTS + 1):
        try:
            formatted_prompt = (
                f"<|im_start|>system\n{cur_sys_prompt}<|im_end|>\n"
                f"<|im_start|>user\n{cur_user_prompt}<|im_end|>\n"
                f"<|im_start|>assistant\n["
            )

            if stream_callback and attempt == 1:
                res = llm.create_completion(
                    prompt=formatted_prompt,
                    max_tokens=cur_tokens,
                    temperature=cur_temp,
                    stop=["<|im_end|>", "<|endoftext|>", "]\n", "]\r\n"],
                    stream=True
                )
                collected = []
                for chunk in res:
                    piece = chunk["choices"][0]["text"]
                    collected.append(piece)
                    stream_callback(piece)
                raw = "[" + "".join(collected)
            else:
                res = llm.create_completion(
                    prompt=formatted_prompt,
                    max_tokens=cur_tokens,
                    temperature=cur_temp,
                    stop=["<|im_end|>", "<|endoftext|>", "]\n", "]\r\n"],
                    stream=False
                )
                raw = "[" + res["choices"][0]["text"]

            # Parse and repair JSON
            parsed = extract_and_repair_json(raw)
            if attempt > 1:
                sys.stderr.write(f"IA: ¡Estructura JSON recuperada con éxito en el intento {attempt}!\n")
            return parsed

        except Exception as err:
            last_error = err
            err_msg = str(err)
            sys.stderr.write(f"Aviso: Fallo al generar/parsear JSON en intento {attempt}/{MAX_ATTEMPTS}: {err_msg}\n")

            if attempt < MAX_ATTEMPTS:
                # Estrategia de corrección progresiva de errores por intento:
                if attempt == 1:
                    # Intento 2: Mitigar truncamiento (más tokens) y estabilizar temperatura a 0.1
                    cur_tokens = int(max_tokens * 1.5)
                    cur_temp = 0.1
                    cur_user_prompt = (
                        f"{user_prompt}\n"
                        f"CORRECTION REQUIREMENT: Previous output failed JSON parsing ({err_msg[:60]}). "
                        f"Output ONLY a raw, syntactically valid JSON array. Double-quote all strings and property names. "
                        f"Do NOT use markdown, do NOT leave trailing commas."
                    )
                    report_progress(0, 0, f"Aviso: Error JSON en intento 1. Ajustando tokens y temperatura (Intento 2/3)...")
                elif attempt == 2:
                    # Intento 3: Temperatura cero (determinismo absoluto), token budget al 1.8x y prompt minimalista estricto
                    cur_tokens = int(max_tokens * 1.8)
                    cur_temp = 0.0
                    cur_sys_prompt = "You are a strict JSON array generator. Output ONLY a valid JSON array matching the request. No explanations, no markdown."
                    cur_user_prompt = f"Generate ONLY a valid JSON array for: {user_prompt}"
                    report_progress(0, 0, f"Aviso: Error JSON en intento 2. Aplicando determinismo máximo (Intento 3/3)...")

    raise ValueError(f"Fallo al generar JSON tras {MAX_ATTEMPTS} intentos con corrección de errores: {last_error}")



def main():
    parser = argparse.ArgumentParser(description="AI Worker for Game Show Center")
    parser.add_argument("--config-file", type=str, help="Path to input JSON configuration file")
    parser.add_argument("--game", type=str, help="Target game name")
    parser.add_argument("--prompt", type=str, help="User prompt/topic")
    parser.add_argument("--count", type=int, default=5, help="Number of items to generate")
    parser.add_argument("--images-per-round", type=int, default=3, help="Images per round (for GeoLocation)")
    parser.add_argument("--mode", type=str, default="add", choices=["add", "overwrite"], help="Insertion mode")
    parser.add_argument("--strictness", type=int, default=5, help="Image strictness threshold on base 10 scale (1 to 10)")
    args = parser.parse_args()

    # Load parameters from file or CLI
    if args.config_file and os.path.exists(args.config_file):
        with open(args.config_file, "r", encoding="utf-8") as f:
            cfg = json.load(f)
            game_name = cfg.get("game", "")
            user_prompt = cfg.get("prompt", "")
            count = int(cfg.get("count", 5))
            images_per_round = int(cfg.get("images_per_round", 3))
            mode = cfg.get("mode", "add")
            image_sources = cfg.get("image_sources", ["wikipedia"])
            strictness = int(cfg.get("strictness", 5))
            existing_items = cfg.get("existing_items", [])
            rag_documents = cfg.get("rag_documents", [])
    else:
        game_name = args.game or ""
        user_prompt = args.prompt or ""
        count = args.count
        images_per_round = args.images_per_round
        mode = args.mode
        image_sources = ["wikipedia"]
        strictness = int(args.strictness or 5)
        existing_items = []
        rag_documents = []

    strictness = max(1, min(10, strictness))

    global GLOBAL_RAG_CONTEXT
    GLOBAL_RAG_CONTEXT = extract_rag_context(rag_documents, user_prompt)
    if GLOBAL_RAG_CONTEXT:
        sys.stderr.write(f"Módulo RAG activo: {len(rag_documents)} documentos PDF cargados ({len(GLOBAL_RAG_CONTEXT)} caracteres de conocimiento).\n")

    # Anti-duplication clause for LLM prompts when in add/append mode
    avoid_clause = ""
    if existing_items and mode == "add":
        clean_existing = [str(x).strip() for x in existing_items if str(x).strip()]
        if clean_existing:
            sample_existing = clean_existing[:30]
            avoid_clause = (
                f"\n\nCRITICAL ANTI-DUPLICATION RULE:\n"
                f"DO NOT repeat, duplicate, or generate similar items to ANY of these existing items already configured in the game:\n"
                f"{', '.join(sample_existing)}\n"
                f"Every item you generate must be completely fresh, new, and distinct from this list."
            )

    rag_prompt_clause = ""
    if GLOBAL_RAG_CONTEXT:
        rag_prompt_clause = (
            f"\n\nADDITIONAL REFERENCE KNOWLEDGE FROM ATTACHED DOCUMENTS:\n"
            f"{GLOBAL_RAG_CONTEXT}\n"
            f"Use and prioritize this knowledge whenever relevant to create high quality, accurate content."
        )

    if not game_name or not user_prompt:
        out = {
            "status": "error",
            "error_code": "INVALID_ARGUMENTS",
            "message": "Se requieren los parámetros 'game' y 'prompt'."
        }
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(1)

    # 1. PROHIBITED THEMES CHECK
    if check_prohibited_theme(user_prompt):
        out = {
            "status": "error",
            "error_code": "PROHIBITED_THEME",
            "message": "Dicha búsqueda no se puede realizar al usar temas prohibidos (Sexo, Drogas, Armas)."
        }
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(0)

    # 2. LOAD GAME AI CONFIG (Requirement 1: Scalable per-game decoupled prompt config)
    ai_cfg = find_game_ai_config(game_name)
    if not ai_cfg or not ai_cfg.get("enabled", False):
        out = {
            "status": "error",
            "error_code": "GAME_NOT_SUPPORTED",
            "message": f"El juego '{game_name}' no posee un archivo de configuración de IA ('ai_prompt.json') habilitado en su carpeta."
        }
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(1)

    gen_type = ai_cfg.get("generation_type", "")
    requires_images = gen_type in ("multi_image_entities", "single_image_entities") or ai_cfg.get("requires_images", False)

    # Requirement 2: Fatal error if images are required but no image data source is configured
    if requires_images and (not image_sources or len(image_sources) == 0):
        sys.stderr.write("ERROR FATAL: Se requieren imágenes para este juego pero no hay ninguna fuente de base de datos configurada.\n")
        out = {
            "status": "error",
            "error_code": "FATAL_NO_IMAGE_SOURCES",
            "message": "ERROR FATAL: No se ha seleccionado ninguna base de datos de imágenes. Debe configurar al menos una fuente de imágenes en los Ajustes de la aplicación."
        }
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(1)

    # 3. LOCATE MODEL
    model_path, mmproj_path = find_model_paths()
    if not model_path or not os.path.exists(model_path):
        out = {
            "status": "error",
            "error_code": "MODEL_NOT_FOUND",
            "message": f"No se encontró el modelo Qwen GGUF en el directorio 'LLM Model/Qwen3.5-9B-GGUF/'."
        }
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(1)

    # 4. INITIALIZE LLM
    from llama_cpp import Llama
    from llama_cpp.llama_chat_format import Qwen25VLChatHandler

    chat_handler = None
    is_multimodal = gen_type in ["multi_image_entities", "single_image_entities"] or ai_cfg.get("vision_enabled", False)
    
    if is_multimodal and mmproj_path and os.path.exists(mmproj_path):
        try:
            chat_handler = Qwen25VLChatHandler(clip_model_path=mmproj_path, verbose=False)
        except Exception as e:
            sys.stderr.write(f"Warning: Could not initialize multimodal projector: {e}\n")

    try:
        report_progress(0, count, "Cargando modelo local Qwen 3.5B...")
        cpu_threads = min(8, max(2, os.cpu_count() or 4))
        llm = Llama(
            model_path=model_path,
            chat_handler=chat_handler,
            n_ctx=2048,
            n_threads=cpu_threads,
            verbose=False
        )
        report_progress(0, count, "Modelo listo. Iniciando generación...")
    except Exception as e:
        out = {
            "status": "error",
            "error_code": "MODEL_LOAD_FAILED",
            "message": f"Error al cargar el modelo de IA: {str(e)}"
        }
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(1)

    # 5. EXECUTE GENERATION BASED ON DECOUPLED ai_prompt.json
    output_data = {}
    copyright_warnings = []

    try:
        # A. QA LIST (e.g. Trivia Quiz)
        if gen_type == "qa_list":
            report_progress(0, count, f"Generando {count} preguntas sobre '{user_prompt}'...")
            system_prompt = ai_cfg.get("system_prompt", "You are an expert trivia question writer.")
            user_msg = ai_cfg.get("user_prompt_template", "Generate exactly {count} trivia questions about the topic: '{prompt}'.").format(count=count, prompt=user_prompt)
            user_msg += avoid_clause + rag_prompt_clause
            tokens = min(1200, count * 90 + 96)
            
            seen_q = [0]
            def on_tq_token(tok):
                if "}" in tok:
                    seen_q[0] += 1
                    c = min(seen_q[0], count)
                    report_progress(c, count, f"Pregunta {c}/{count} generada...")

            q_list = generate_text_llm(llm, system_prompt, user_msg, max_tokens=tokens, stream_callback=on_tq_token)
            
            seen_existing_q = set(s.lower().strip() for s in existing_items)
            formatted_pool = []
            for item in q_list:
                if isinstance(item, dict) and "question" in item and "answer" in item:
                    q_str = str(item["question"]).strip()
                    if q_str.lower() in seen_existing_q:
                        continue
                    seen_existing_q.add(q_str.lower())
                    formatted_pool.append({
                        "question": q_str,
                        "answer": str(item["answer"]).strip()
                    })
            output_data = {
                "questionPool": formatted_pool[:count]
            }
            report_progress(count, count, f"¡{len(formatted_pool[:count])} preguntas generadas!")

        # B. WORD LIST (e.g. Hangman - Non-repeating unique words)
        elif gen_type == "word_list":
            cand_count = max(count * 3, count + 15)
            report_progress(0, count, f"Generando palabras únicas sobre '{user_prompt}'...")
            system_prompt = ai_cfg.get("system_prompt", "You are a word puzzle creator for Hangman.")
            user_msg = ai_cfg.get("user_prompt_template", "Generate {cand_count} distinct, non-repeating words strictly related to '{prompt}'.").format(cand_count=cand_count, count=count, prompt=user_prompt)
            user_msg += avoid_clause + rag_prompt_clause
            tokens = min(768, cand_count * 22 + 64)
            
            seen_w = [0]
            def on_hm_token(tok):
                if "," in tok or "\n" in tok:
                    seen_w[0] += 1
                    c = min(seen_w[0], count)
                    report_progress(c, count, f"Palabra {c}/{count} generada...")

            w_list = generate_text_llm(llm, system_prompt, user_msg, max_tokens=tokens, stream_callback=on_hm_token)
            
            clean_words = []
            seen_words_set = set()
            for ew in existing_items:
                clean_ew = re.sub(r"[^A-Z]", "", str(ew).upper().replace("Á","A").replace("É","E").replace("Í","I").replace("Ó","O").replace("Ú","U").replace("Ñ","N"))
                if clean_ew:
                    seen_words_set.add(clean_ew)

            for w in w_list:
                if isinstance(w, str):
                    clean_w = re.sub(r"[^A-Z]", "", w.upper().replace("Á","A").replace("É","E").replace("Í","I").replace("Ó","O").replace("Ú","U").replace("Ñ","N"))
                    if len(clean_w) >= 4 and clean_w not in seen_words_set:
                        seen_words_set.add(clean_w)
                        clean_words.append(clean_w)

            # Si faltan palabras para alcanzar count debido a filtrado de duplicados, solicitar candidatos adicionales únicos
            if len(clean_words) < count:
                needed = count - len(clean_words)
                sys.stderr.write(f"Solicitando {needed} palabras únicas adicionales para completar las {count} requeridas...\n")
                sample_seen_words = list(seen_words_set)[:30]
                more_prompt = f"Generate {needed * 3} distinct words related to '{user_prompt}'. DO NOT use any of these existing words: {', '.join(sample_seen_words)}." + rag_prompt_clause
                try:
                    more_list = generate_text_llm(llm, system_prompt, more_prompt, max_tokens=min(512, needed * 30 + 32))
                    for w in more_list:
                        if isinstance(w, str):
                            clean_w = re.sub(r"[^A-Z]", "", w.upper().replace("Á","A").replace("É","E").replace("Í","I").replace("Ó","O").replace("Ú","U").replace("Ñ","N"))
                            if len(clean_w) >= 4 and clean_w not in seen_words_set:
                                seen_words_set.add(clean_w)
                                clean_words.append(clean_w)
                                if len(clean_words) >= count:
                                    break
                except Exception as ex_more:
                    sys.stderr.write(f"Aviso: no se pudieron obtener palabras adicionales: {ex_more}\n")

            output_data = {
                "wordPool": clean_words[:count]
            }
            report_progress(count, count, f"¡{len(clean_words[:count])} palabras únicas generadas!")

        # C. HIERARCHICAL CATEGORIES (e.g. Topic Takedown)
        elif gen_type == "hierarchical_categories":
            cat_count = min(6, max(1, count))
            report_progress(0, cat_count, f"Creando {cat_count} categorías para '{user_prompt}'...")
            system_prompt_cats = ai_cfg.get("system_prompt_categories", "Generate distinct category names related to the given topic.")
            user_msg_cats = ai_cfg.get("user_prompt_template_categories", "Generate exactly {cat_count} category names related to '{prompt}'.").format(cat_count=cat_count, prompt=user_prompt)
            user_msg_cats += avoid_clause + rag_prompt_clause
            cat_names = generate_text_llm(llm, system_prompt_cats, user_msg_cats, max_tokens=150)
            
            seen_existing_cats = set(s.lower().strip() for s in existing_items)
            formatted_cats = []
            for idx, cat_name in enumerate(cat_names):
                if len(formatted_cats) >= cat_count:
                    break
                if not isinstance(cat_name, str) or not cat_name.strip():
                    continue
                c_clean = cat_name.strip()
                if c_clean.lower() in seen_existing_cats:
                    continue
                seen_existing_cats.add(c_clean.lower())
                report_progress(len(formatted_cats), cat_count, f"Categoría {len(formatted_cats) + 1}/{cat_count}: Generando preguntas de '{c_clean}'...")
                q_system = ai_cfg.get("system_prompt_questions", "Generate exactly 4 trivia questions for category '{category}'.").replace("{category}", c_clean)
                q_user = ai_cfg.get("user_prompt_template_questions", "Questions for category: {category}.").replace("{category}", c_clean)
                if rag_prompt_clause:
                    q_user += rag_prompt_clause
                try:
                    raw_qs = generate_text_llm(llm, q_system, q_user, max_tokens=450)
                except Exception as ex:
                    sys.stderr.write(f"Warning generating questions for {c_clean}: {ex}\n")
                    raw_qs = []
                
                questions = []
                if isinstance(raw_qs, list):
                    for i, q in enumerate(raw_qs):
                        if isinstance(q, dict) and "question" in q and "answer" in q:
                            questions.append({
                                "difficulty": i + 1,
                                "question": str(q["question"]).strip(),
                                "answer": str(q["answer"]).strip(),
                                "imageUrl": "",
                                "audioUrl": ""
                            })
                while len(questions) < 4:
                    diff = len(questions) + 1
                    questions.append({
                        "difficulty": diff,
                        "question": f"Pregunta {diff} de {c_clean}?",
                        "answer": "Respuesta",
                        "imageUrl": "",
                        "audioUrl": ""
                    })
                formatted_cats.append({
                    "categoryName": c_clean,
                    "category_name": c_clean,
                    "questions": questions[:4]
                })
                report_progress(len(formatted_cats), cat_count, f"Categoría {len(formatted_cats)}/{cat_count}: '{c_clean}' completada")

            output_data = {
                "numCategories": len(formatted_cats),
                "num_categories": len(formatted_cats),
                "questionsPerCategory": 4,
                "questions_per_category": 4,
                "categories": formatted_cats
            }
            report_progress(cat_count, cat_count, "¡Todas las categorías listas!")

        # D. MULTI-IMAGE ENTITIES (e.g. GeoLocation - Open to Any Topic)
        elif gen_type == "multi_image_entities":
            system_prompt = ai_cfg.get("system_prompt", "Generate visually recognizable entities.")
            user_msg_tmpl = ai_cfg.get("user_prompt_template", "Generate {cand_count} recognizable entities related to '{prompt}'.")
            vision_template = ai_cfg.get("vision_prompt_template", "")
            similarity_threshold = int(ai_cfg.get("similarity_dhash_threshold", 12))
            min_imgs = max(1, images_per_round)
            ACCEPT_THRESHOLD = 10.0 + (strictness - 1) * 8.0

            locations_res = []
            all_used_images = []
            seen_locs = set(s.lower().strip() for s in existing_items)
            attempt_round = 1
            max_rounds = 10

            report_progress(0, count, f"Iniciando búsqueda de {count} elementos para '{user_prompt}'...")

            while len(locations_res) < count and attempt_round <= max_rounds:
                needed = count - len(locations_res)
                batch_size = min(40, max(needed + 6, needed * 2))

                if attempt_round == 1:
                    report_progress(0, count, f"Identificando elementos para '{user_prompt}'...")
                    user_msg = user_msg_tmpl.format(cand_count=batch_size, count=count, prompt=user_prompt) + avoid_clause + rag_prompt_clause
                else:
                    report_progress(len(locations_res), count, f"Completando ({len(locations_res)}/{count}): Solicitando {needed} elementos adicionales a la IA...")
                    sample_seen = list(seen_locs)[:25]
                    user_msg = (
                        f"{user_msg_tmpl.format(cand_count=batch_size, count=needed, prompt=user_prompt)}\n"
                        f"IMPORTANT: DO NOT repeat any of these already evaluated or existing items: {', '.join(sample_seen)}. "
                        f"Provide ONLY new, distinct, and visually recognizable items."
                    ) + rag_prompt_clause

                try:
                    loc_names = generate_text_llm(llm, system_prompt, user_msg, max_tokens=1200)
                except Exception as ex_gen:
                    sys.stderr.write(f"Aviso al generar lote {attempt_round} de ubicaciones: {ex_gen}\n")
                    loc_names = []

                if not isinstance(loc_names, list) or not loc_names:
                    attempt_round += 1
                    continue

                for loc_name in loc_names:
                    if len(locations_res) >= count:
                        break
                    if not isinstance(loc_name, str) or not loc_name.strip():
                        continue
                    loc_clean = loc_name.strip()
                    loc_key = loc_clean.lower()
                    if loc_key in seen_locs:
                        continue
                    seen_locs.add(loc_key)

                    cur_idx = len(locations_res)
                    report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Buscando fotos de '{loc_clean}'...")

                    candidates = fetch_candidate_images(game_name, loc_clean, theme=user_prompt, limit=max(14, min_imgs * 5), image_sources=image_sources, llm=llm)
                    verified_urls = []
                    verified_hashes = []

                    for c_idx, cand in enumerate(candidates):
                        url = cand["url"]
                        src_label = cand.get("license", "Fuente")
                        report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Evaluando foto {c_idx + 1}/{len(candidates)} de '{loc_clean}' ({src_label})...")
                        ok, img_bytes, score = verify_image_with_vision(llm, url, loc_clean, theme=user_prompt, custom_prompt_template=vision_template, timeout_sec=35.0, cand_meta=cand, strictness=strictness)
                        if ok and img_bytes and score >= ACCEPT_THRESHOLD:
                            # Check visual similarity against previously accepted images for this entity
                            try:
                                from PIL import Image
                                from io import BytesIO
                                cand_pil = Image.open(BytesIO(img_bytes))
                                too_sim, h_or_dist = is_image_too_similar(cand_pil, verified_hashes, threshold=similarity_threshold)
                            except Exception:
                                too_sim, h_or_dist = False, 0

                            if too_sim:
                                report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Foto aprobada ({score:.0f}) pero de ángulo similar. Buscando otro ángulo...")
                                continue

                            local_ref = save_verified_image(img_bytes, game_name, loc_clean, index=len(verified_urls) + 1)
                            if local_ref:
                                verified_urls.append(local_ref)
                                verified_hashes.append(h_or_dist)
                                all_used_images.append(local_ref)
                                if cand.get("copyright_warning"):
                                    copyright_warnings.append(local_ref)
                                report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Foto {len(verified_urls)}/{min_imgs} aceptada (Score: {score:.0f} >= {ACCEPT_THRESHOLD:.0f} [Nivel {strictness}/10])")

                            if len(verified_urls) >= min_imgs:
                                break
                        else:
                            report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Foto {c_idx + 1} no alcanzó el umbral ({score:.0f} < {ACCEPT_THRESHOLD:.0f} [Nivel {strictness}/10]). Probando siguiente...")

                    if len(verified_urls) >= min_imgs:
                        locations_res.append({
                            "location_name": loc_clean,
                            "images": verified_urls[:min_imgs]
                        })
                        report_progress(len(locations_res), count, f"Elemento {len(locations_res)}/{count}: '{loc_clean}' completado ({len(verified_urls[:min_imgs])} fotos)")
                    else:
                        report_progress(cur_idx, count, f"'{loc_clean}' no tuvo suficientes fotos que superaran el umbral. Continuando con otro elemento...")

                attempt_round += 1

            output_data = {
                "locations": locations_res,
                "images_per_round": min_imgs,
                "rounds_per_player": max(1, len(locations_res))
            }
            # Requirement 4: Purge unreferenced images from disk
            purge_unused_images(game_name, all_used_images)
            report_progress(len(locations_res), count, f"¡{len(locations_res)} elementos e imágenes verificadas!")

        # E. SINGLE-IMAGE ENTITIES (e.g. Guess Character, Snap Solve)
        elif gen_type == "single_image_entities":
            media_field = ai_cfg.get("media_field", "media_pool")
            system_prompt = ai_cfg.get("system_prompt", "Generate recognizable entities or characters.")
            user_msg_tmpl = ai_cfg.get("user_prompt_template", "Generate {cand_count} recognizable entities related to '{prompt}'.")
            vision_template = ai_cfg.get("vision_prompt_template", "")
            ACCEPT_THRESHOLD = 10.0 + (strictness - 1) * 8.0

            media_pool = []
            seen_items = set(s.lower().strip() for s in existing_items)
            attempt_round = 1
            max_rounds = 10

            report_progress(0, count, f"Iniciando búsqueda de {count} elementos para '{user_prompt}'...")

            while len(media_pool) < count and attempt_round <= max_rounds:
                needed = count - len(media_pool)
                batch_size = min(40, max(needed + 6, needed * 2))

                if attempt_round == 1:
                    report_progress(0, count, f"Identificando elementos para '{user_prompt}'...")
                    user_msg = user_msg_tmpl.format(cand_count=batch_size, count=count, prompt=user_prompt) + avoid_clause + rag_prompt_clause
                else:
                    report_progress(len(media_pool), count, f"Completando ({len(media_pool)}/{count}): Solicitando {needed} elementos adicionales a la IA...")
                    sample_seen = list(seen_items)[:25]
                    user_msg = (
                        f"{user_msg_tmpl.format(cand_count=batch_size, count=needed, prompt=user_prompt)}\n"
                        f"IMPORTANT: DO NOT repeat any of these already evaluated or existing items: {', '.join(sample_seen)}. "
                        f"Provide ONLY new, distinct, and visually recognizable items."
                    ) + rag_prompt_clause

                try:
                    names = generate_text_llm(llm, system_prompt, user_msg, max_tokens=1200)
                except Exception as ex_gen:
                    sys.stderr.write(f"Aviso al generar lote {attempt_round} de nombres: {ex_gen}\n")
                    names = []

                if not isinstance(names, list) or not names:
                    attempt_round += 1
                    continue

                for name in names:
                    if len(media_pool) >= count:
                        break
                    if not isinstance(name, str) or not name.strip():
                        continue
                    item_clean = name.strip()
                    item_key = item_clean.lower()
                    if item_key in seen_items:
                        continue
                    seen_items.add(item_key)

                    cur_idx = len(media_pool)
                    report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Buscando imagen de '{item_clean}' en todas las fuentes...")
                    candidates = fetch_candidate_images(game_name, item_clean, theme=user_prompt, limit=14, image_sources=image_sources, llm=llm)

                    accepted = False
                    for c_idx, cand in enumerate(candidates):
                        url = cand["url"]
                        src_label = cand.get("license", "Fuente")
                        report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Evaluando imagen {c_idx + 1}/{len(candidates)} de '{item_clean}' ({src_label})...")
                        ok, img_bytes, score = verify_image_with_vision(llm, url, item_clean, theme=user_prompt, custom_prompt_template=vision_template, timeout_sec=35.0, cand_meta=cand, strictness=strictness)
                        if ok and img_bytes and score >= ACCEPT_THRESHOLD:
                            chosen_ref = save_verified_image(img_bytes, game_name, item_clean, index=1)
                            if chosen_ref:
                                media_pool.append(chosen_ref)
                                if cand.get("copyright_warning"):
                                    copyright_warnings.append(chosen_ref)
                                report_progress(len(media_pool), count, f"Elemento {len(media_pool)}/{count}: '{item_clean}' aceptado (Score: {score:.0f} >= {ACCEPT_THRESHOLD:.0f} [Nivel {strictness}/10])")
                                accepted = True
                                break
                        else:
                            report_progress(cur_idx, count, f"Elemento {cur_idx + 1}/{count}: Imagen {c_idx + 1} no superó el umbral ({score:.0f} < {ACCEPT_THRESHOLD:.0f} [Nivel {strictness}/10]). Probando siguiente...")

                    if not accepted:
                        report_progress(cur_idx, count, f"'{item_clean}' no superó el umbral en ninguna imagen. Continuando con otro elemento...")

                attempt_round += 1

            output_data = {
                media_field: media_pool,
                "rounds_per_player": max(1, min(len(media_pool), count))
            }
            # Requirement 4: Purge unreferenced images from disk
            purge_unused_images(game_name, media_pool)
            report_progress(len(media_pool), count, f"¡{len(media_pool)} elementos verificados!")

        # F. TIMELINE EVENTS (e.g. TimeLine)
        elif gen_type == "timeline_events":
            report_progress(0, count, f"Generando hitos cronológicos sobre '{user_prompt}'...")
            system_prompt = ai_cfg.get("system_prompt", "You are an educational history and science expert writer for Game Show Center. Generate chronological milestone events with accurate historical/scientific years, titles, and short descriptions. Respond ONLY with a valid JSON array of objects, each containing: 'title' (string), 'year' (integer, negative for BCE), and 'description' (string, 1 concise sentence).")
            user_msg_tmpl = ai_cfg.get("user_prompt_template", "Generate exactly {count} distinct chronological milestone events related to: '{prompt}'.")

            formatted_events = []
            seen_titles = set()
            attempt_round = 1

            while len(formatted_events) < count and attempt_round <= 5:
                needed = count - len(formatted_events)
                tokens = min(1500, needed * 120 + 128)
                if attempt_round == 1:
                    user_msg = user_msg_tmpl.format(count=count, prompt=user_prompt)
                else:
                    sample_titles = list(seen_titles)[:15]
                    user_msg = f"{user_msg_tmpl.format(count=needed, prompt=user_prompt)}\nDO NOT repeat any of these already used milestone events: {', '.join(sample_titles)}."

                try:
                    ev_list = generate_text_llm(llm, system_prompt, user_msg, max_tokens=tokens)
                except Exception as ex_tl:
                    sys.stderr.write(f"Aviso en generación de eventos históricos lote {attempt_round}: {ex_tl}\n")
                    ev_list = []

                if isinstance(ev_list, list):
                    for item in ev_list:
                        if isinstance(item, dict) and "title" in item and "year" in item:
                            t_str = str(item["title"]).strip()
                            t_key = t_str.lower()
                            if not t_str or t_key in seen_titles:
                                continue
                            try:
                                year_val = int(item["year"])
                            except Exception:
                                continue
                            seen_titles.add(t_key)
                            formatted_events.append({
                                "id": f"ev_{len(formatted_events) + 1}",
                                "title": t_str,
                                "year": year_val,
                                "description": str(item.get("description", "")).strip()
                            })
                            report_progress(len(formatted_events), count, f"Evento histórico {len(formatted_events)}/{count}: '{t_str}'")
                            if len(formatted_events) >= count:
                                break

                attempt_round += 1

            formatted_events.sort(key=lambda x: x["year"])
            output_data = {
                "events": formatted_events[:count]
            }
            report_progress(len(formatted_events[:count]), count, f"¡{len(formatted_events[:count])} hitos históricos listos!")

        else:
            raise ValueError(f"Tipo de generación no soportado ('{gen_type}') en la configuración de '{game_name}'")

        final_response = {
            "status": "success",
            "game": game_name,
            "mode": mode,
            "data": output_data,
            "copyright_warnings": list(set(copyright_warnings))
        }
        print(json.dumps(final_response, ensure_ascii=False))
        sys.exit(0)

    except Exception as e:
        err_response = {
            "status": "error",
            "error_code": "GENERATION_FAILED",
            "message": f"Error durante la generación con IA: {str(e)}"
        }
        print(json.dumps(err_response, ensure_ascii=False))
        sys.exit(1)

if __name__ == "__main__":
    main()
