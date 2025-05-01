import os

def find_get_lines_with_info(file_path):
    """Returns list of lines with .get, including line number and file path."""
    matches = []
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            for idx, line in enumerate(file, start=1):
                if '.get(' in line:
                    matches.append((file_path, idx, line.strip()))
    except (UnicodeDecodeError, FileNotFoundError):
        pass  # Skip unreadable files
    return matches

def walk_and_search(directory_path):
    results = []
    for root, dirs, files in os.walk(directory_path):
        for file in files:
            if file.endswith('.java'):
                full_path = os.path.join(root, file)
                results.extend(find_get_lines_with_info(full_path))
    return results

def main():
    directory_path = os.path.dirname(os.path.realpath(__file__))
    matches = walk_and_search(directory_path)

    if matches:
        print("Found lines containing '.get':\n")
        for path, line_number, code in matches:
            print(f"{path}:{line_number}: {code}")
    else:
        print("No '.get' usages found.")

if __name__ == '__main__':
    main()
