#!/usr/bin/env python3
"""
Android strings.xml Validation & Auto-Fix Tool

Checks Android resource translation files for AAPT2 compilation & runtime string formatting issues:
1. XML Syntax Errors
2. Duplicate Keys
3. Unescaped Single Quotes (')
4. Unescaped Percentage Signs (%)
5. Format Specifier Mismatches (compared against values/strings.xml)
6. Invalid Escape Sequences

Usage:
  python3 scripts/validate_strings.py           # Validate all strings.xml files
  python3 scripts/validate_strings.py --fix     # Validate and auto-fix simple escaping issues
"""

import sys
import os
import re
import glob
import argparse
import xml.etree.ElementTree as ET

# ANSI Colors
RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
BOLD = "\033[1m"
RESET = "\033[0m"

# Valid Android string backslash escapes: \', \", \\, \n, \t, \?, \@, \uXXXX
VALID_ESCAPE_REGEX = re.compile(r'\\(\'|"|\\|n|t|\?|@|u[0-9a-fA-F]{4})')
INVALID_ESCAPE_REGEX = re.compile(r'\\([^\'"\\nt?@u])|\\u(?![0-9a-fA-F]{4})')

# Format specifiers regex: %1$s, %2$d, %s, %d, %1$.1f, %% etc.
FORMAT_SPEC_REGEX = re.compile(r'%(?:(\d+)\$)?[-+ #0(]*\d*(?:\.\d+)?[a-zA-Z%]')

def get_base_strings(base_path):
    """Loads base strings from values/strings.xml into a dict: {key: (raw_val, placeholders)}"""
    if not os.path.exists(base_path):
        return {}
    
    base_data = {}
    try:
        tree = ET.parse(base_path)
        root = tree.getroot()
        for elem in root.findall('string'):
            name = elem.get('name')
            if not name:
                continue
            # Get raw inner XML content
            raw_text = (elem.text or "")
            for child in elem:
                raw_text += ET.tostring(child, encoding='unicode')
            if elem.tail:
                raw_text += elem.tail
            
            # Find positional placeholders e.g. %1$s, %2$d
            specs = FORMAT_SPEC_REGEX.findall(raw_text)
            # Find exact specifier matches
            full_specs = re.findall(r'%(?:\d+\$)?[-+ #0(]*\d*(?:\.\d+)?[a-zA-Z]', raw_text)
            full_specs_no_percent = [s for s in full_specs if s != '%%']
            base_data[name] = {
                'raw': raw_text,
                'specifiers': sorted(full_specs_no_percent)
            }
    except Exception as e:
        print(f"{RED}Error parsing base strings file {base_path}: {e}{RESET}")
    return base_data

def check_string_value(key, value, is_formatted_attribute):
    """Checks string content for escaping and formatting issues."""
    issues = []

    # 1. Unescaped Single Quotes
    # Single quotes are valid unescaped ONLY if the whole string is enclosed in double quotes "..."
    stripped = value.strip()
    is_double_quoted = len(stripped) >= 2 and stripped.startswith('"') and stripped.endswith('"')

    if not is_double_quoted:
        # Search for unescaped single quotes: ' not preceded by \
        # Ignore \'
        unescaped_single_quotes = re.findall(r"(?<!\\)'", value)
        if unescaped_single_quotes:
            issues.append({
                'type': 'UNESCAPED_SINGLE_QUOTE',
                'msg': "Unescaped single quote (') found. Must be escaped as \\' or enclosed in double quotes.",
                'fixable': True
            })

    # 2. Percentage Sign Escaping Check
    # If formatted="false" is explicitly set, percentage signs do not need escaping for formatting.
    if is_formatted_attribute != "false":
        # Find format specifiers
        format_specs = re.findall(r'%(?:\d+\$)?[-+ #0(]*\d*(?:\.\d+)?[a-zA-Z]', value)
        has_placeholders = len(format_specs) > 0

        # Check for unescaped %
        # Unescaped % is any % that is NOT part of a valid format specifier and NOT %%
        # Replace %% with placeholder to test remaining %
        temp_val = value.replace('%%', 'DOUBLE_PERCENT_MARKER')
        # Remove valid format specifiers
        temp_val = re.sub(r'%(?:\d+\$)?[-+ #0(]*\d*(?:\.\d+)?[a-zA-Z]', 'VALID_SPEC_MARKER', temp_val)
        if '%' in temp_val:
            if has_placeholders:
                issues.append({
                    'type': 'UNESCAPED_PERCENT',
                    'msg': "Unescaped '%' sign found in a formatted string. Must be escaped as '%%'.",
                    'fixable': True
                })

    # 3. Invalid escape sequence check (e.g. \e or \x)
    invalid_escapes = INVALID_ESCAPE_REGEX.findall(value)
    if invalid_escapes:
        issues.append({
            'type': 'INVALID_ESCAPE',
            'msg': f"Invalid backslash escape sequence found: {invalid_escapes}",
            'fixable': False
        })

    return issues

def validate_file(file_path, base_strings, auto_fix=False):
    """Validates a single strings.xml file."""
    issues_found = []
    file_modified = False

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        content = "".join(lines)
    except Exception as e:
        return [f"Unable to read file: {e}"], False

    # Check XML Syntax
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
    except ET.ParseError as pe:
        line_num, col = pe.position if hasattr(pe, 'position') else (0, 0)
        return [{
            'line': line_num,
            'key': 'XML_SYNTAX',
            'type': 'XML_SYNTAX_ERROR',
            'msg': f"XML syntax error: {pe}",
            'is_error': True,
            'fixable': False
        }], False

    seen_keys = set()

    # Parse raw lines to preserve comments/formatting when auto-fixing
    # Also extract exact string elements with line numbers
    string_tag_pattern = re.compile(
        r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>',
        re.DOTALL
    )

    new_lines = list(lines)

    for idx, line in enumerate(lines, start=1):
        for match in string_tag_pattern.finditer(line):
            key = match.group(1)
            attrs = match.group(2)
            val = match.group(3)

            # Duplicate key check
            if key in seen_keys:
                issues_found.append({
                    'line': idx,
                    'key': key,
                    'type': 'DUPLICATE_KEY',
                    'msg': f"Duplicate string resource key '{key}' in same file.",
                    'is_error': True,
                    'fixable': False
                })
            seen_keys.add(key)

            # Formatted attribute check
            is_formatted = "false" if 'formatted="false"' in attrs else "true"

            item_issues = check_string_value(key, val, is_formatted)

            # Format Specifier Comparison against base strings
            if key in base_strings:
                base_specs = base_strings[key]['specifiers']
                if base_specs:
                    trans_specs = re.findall(r'%(?:\d+\$)?[-+ #0(]*\d*(?:\.\d+)?[a-zA-Z]', val)
                    trans_specs_no_percent = [s for s in trans_specs if s != '%%']
                    
                    # Check if positional indices match
                    base_indices = sorted(re.findall(r'%(\d+)\$', "".join(base_specs)))
                    trans_indices = sorted(re.findall(r'%(\d+)\$', "".join(trans_specs_no_percent)))

                    if base_indices and base_indices != trans_indices:
                        item_issues.append({
                            'type': 'SPECIFIER_MISMATCH',
                            'is_error': False, # Warning by default
                            'msg': f"Placeholder specifiers mismatch base string. Expected positional specifiers {base_indices}, found {trans_indices}.",
                            'fixable': False
                        })

            # Auto-Fix handling
            fixed_val = val
            fixed_this_item = False
            for issue in item_issues:
                issue['line'] = idx
                issue['key'] = key
                if 'is_error' not in issue:
                    issue['is_error'] = True
                issues_found.append(issue)

                if auto_fix and issue.get('fixable'):
                    if issue['type'] == 'UNESCAPED_SINGLE_QUOTE':
                        # Fix unescaped single quotes
                        fixed_val = re.sub(r"(?<!\\)'", r"\'", fixed_val)
                        fixed_this_item = True
                    elif issue['type'] == 'UNESCAPED_PERCENT':
                        # Fix unescaped % sign: replace single % with %% unless it's already %% or part of valid format specifier
                        # Protect valid specifiers and double percents
                        temp = fixed_val.replace('%%', 'DOUBLE_PERCENT_MARKER')
                        temp = re.sub(r'(%(?:\d+\$)?[-+ #0(]*\d*(?:\.\d+)?[a-zA-Z])', r'VALID_SPEC_\1', temp)
                        temp = temp.replace('%', '%%')
                        temp = temp.replace('VALID_SPEC_', '')
                        fixed_val = temp.replace('DOUBLE_PERCENT_MARKER', '%%')
                        fixed_this_item = True

            if auto_fix and fixed_this_item:
                old_tag = match.group(0)
                new_tag = f'<string name="{key}"{attrs}>{fixed_val}</string>'
                new_lines[idx - 1] = new_lines[idx - 1].replace(old_tag, new_tag)
                file_modified = True

    if auto_fix and file_modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)

    return issues_found, file_modified

def check_or_update_locales_config(res_dir, auto_fix=False):
    config_file = os.path.join(res_dir, "xml", "locales_config.xml")
    if not os.path.exists(config_file):
        return

    files = glob.glob(os.path.join(res_dir, "values*", "strings.xml"))
    locales = set()
    for f in files:
        dir_name = os.path.basename(os.path.dirname(f))
        if dir_name == "values":
            locales.add("en")
        else:
            code = dir_name.replace("values-", "")
            if "-r" in code:
                code = code.replace("-r", "-")
            locales.add(code)

    sorted_locales = sorted(locales)

    try:
        tree = ET.parse(config_file)
        root = tree.getroot()
        existing = set()
        for elem in root.findall('locale'):
            val = elem.get('{http://schemas.android.com/apk/res/android}name') or elem.get('android:name') or elem.get('name')
            if val:
                existing.add(val)

        missing = set(sorted_locales) - existing
        if missing:
            if auto_fix:
                lines = ['<?xml version="1.0" encoding="utf-8"?>\n', '<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n']
                for loc in sorted_locales:
                    lines.append(f'    <locale android:name="{loc}" />\n')
                lines.append('</locale-config>\n')
                with open(config_file, 'w', encoding='utf-8') as f:
                    f.writelines(lines)
                print(f"{BOLD}{GREEN}🔧 Updated locales_config.xml with missing locales: {sorted(missing)}{RESET}\n")
            else:
                print(f"{BOLD}{YELLOW}⚠️  locales_config.xml is missing locales: {sorted(missing)}{RESET}\n")
    except Exception as e:
        pass

def main():
    parser = argparse.ArgumentParser(description="Validate Android translation strings.xml files.")
    parser.add_argument('--fix', action='store_true', help="Auto-fix simple escaping issues (unescaped ' and %%)")
    parser.add_argument('--strict', action='store_true', help="Treat placeholder warnings as errors")
    parser.add_argument('--path', type=str, default='app/src/main/res', help="Path to res directory")
    args = parser.parse_args()

    res_dir = os.path.abspath(args.path)
    if not os.path.exists(res_dir):
        print(f"{RED}Error: Directory {res_dir} does not exist.{RESET}")
        sys.exit(1)

    check_or_update_locales_config(res_dir, auto_fix=args.fix)

    base_strings_file = os.path.join(res_dir, "values", "strings.xml")
    base_strings = get_base_strings(base_strings_file)

    files_to_check = sorted(glob.glob(os.path.join(res_dir, "values*", "strings.xml")))

    if not files_to_check:
        print(f"{YELLOW}No strings.xml files found in {res_dir}{RESET}")
        sys.exit(0)

    total_errors = 0
    total_warnings = 0
    files_with_issues = 0
    fixed_files_count = 0

    print(f"{BOLD}{CYAN}Scanning {len(files_to_check)} strings.xml translation files...{RESET}\n")

    for file_path in files_to_check:
        rel_path = os.path.relpath(file_path, start=os.getcwd())
        issues, was_modified = validate_file(file_path, base_strings, auto_fix=args.fix)

        if was_modified:
            fixed_files_count += 1

        if issues:
            errors = [i for i in issues if i['is_error'] or args.strict]
            warnings = [i for i in issues if not (i['is_error'] or args.strict)]

            total_errors += len(errors)
            total_warnings += len(warnings)
            files_with_issues += 1

            status_color = RED if errors else YELLOW
            status_icon = "❌" if errors else "⚠️ "

            print(f"{BOLD}{status_color}{status_icon} {rel_path}{RESET}")
            for issue in issues:
                line_str = f"Line {issue.get('line', '?')}"
                key_str = f" [{issue.get('key', '')}]" if issue.get('key') else ""
                tag = f"{RED}ERROR{RESET}" if (issue['is_error'] or args.strict) else f"{YELLOW}WARN{RESET}"
                print(f"  {tag} {line_str}{key_str}: {issue['msg']}")
                if args.fix and issue.get('fixable'):
                    print(f"    {GREEN}↳ [AUTO-FIXED]{RESET}")
            print()

    if total_errors == 0:
        if total_warnings > 0:
            print(f"{BOLD}{GREEN}✅ All {len(files_to_check)} translation files passed validation with 0 errors!{RESET} ({YELLOW}{total_warnings} warnings{RESET})")
        else:
            print(f"{BOLD}{GREEN}✅ All {len(files_to_check)} translation files passed validation with 0 issues!{RESET}")
        sys.exit(0)
    else:
        if args.fix and fixed_files_count > 0:
            print(f"{BOLD}{GREEN}🔧 Auto-fixed issues in {fixed_files_count} file(s).{RESET}")
            print(f"{BOLD}{YELLOW}Re-running validation check...{RESET}\n")
            # Re-run check after fixes
            remaining_errors = 0
            for file_path in files_to_check:
                issues, _ = validate_file(file_path, base_strings, auto_fix=False)
                errors = [i for i in issues if i['is_error'] or args.strict]
                remaining_errors += len(errors)
            if remaining_errors == 0:
                print(f"{BOLD}{GREEN}✅ All remaining errors resolved!{RESET}")
                sys.exit(0)
            else:
                print(f"{BOLD}{RED}❌ {remaining_errors} errors require manual attention.{RESET}")
                sys.exit(1)
        else:
            print(f"{BOLD}{RED}Found {total_errors} error(s) and {total_warnings} warning(s) across {files_with_issues} file(s).{RESET}")
            print(f"{CYAN}Tip: Run 'python3 scripts/validate_strings.py --fix' to auto-fix quote and percent escaping issues.{RESET}")
            sys.exit(1)

if __name__ == '__main__':
    main()
