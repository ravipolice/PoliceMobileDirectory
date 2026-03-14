import sys
import os

def parse():
    # Try multiple encodings
    encodings = ['utf-16', 'utf-8', 'latin-1']
    content = None
    for enc in encodings:
        try:
            with open('build_plain.log', 'r', encoding=enc) as f:
                content = f.read()
                print(f"Read successful with {enc}")
                break
        except Exception:
            continue
    
    if content is None:
        print("Failed to read log with any encoding.")
        return

    lines = content.splitlines()
    errors = []
    for line in lines:
        if ('error:' in line.lower() or 'e: ' in line or 'unresolved' in line.lower()) and 'file:' in line.lower():
            # Clean up the line for display
            if 'file:///' in line:
                line = line.split('file:///')[-1]
            errors.append(line.strip())
    
    if not errors:
        # If no file-specific errors, maybe just look for anything with error:
        for line in lines:
            if 'error:' in line.lower() or 'failure:' in line.lower():
                errors.append(line.strip())

    if not errors:
        print("No errors found in log.")
        return

    # Print first 50 errors
    for err in errors[:50]:
        print(err)

if __name__ == "__main__":
    parse()
