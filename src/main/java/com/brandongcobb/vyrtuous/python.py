import os

# Function to replace text in a single file
def replace_in_file(file_path, old_string, new_string):
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            file_contents = file.read()

        # Replace old string with new string
        new_contents = file_contents.replace(old_string, new_string)

        # Write the modified contents back to the file
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(new_contents)

        print(f"Replaced in file: {file_path}")

    except Exception as e:
        print(f"Error processing file {file_path}: {e}")

# Function to walk through the directory recursively and replace text
def replace_in_directory(directory, old_string, new_string):
    for root, dirs, files in os.walk(directory):
        for file in files:
            file_path = os.path.join(root, file)

            # Check if the file is a text-based file (you can customize this condition)
            if file.endswith(('.java', '.txt', '.py')):  # Modify this as per your file types
                replace_in_file(file_path, old_string, new_string)

# Get the folder where this Python script is located
script_directory = os.path.dirname(os.path.realpath(__file__))

# Specify the strings to replace
old_string = 'completeGetConfigStringValue'
new_string = 'completeGetConfigStringValue'

# Start the replacement process in the script's directory
replace_in_directory(script_directory, old_string, new_string)
