import os

khoi_file = r"C:\Users\ADMIN\Downloads\khoi_utf8.sql"
thuan_file = r"C:\Users\ADMIN\Downloads\thuan.sql"
output_file = r"C:\Users\ADMIN\Downloads\Publication-Trend-Tracking-System\database\final_schema.sql"

with open(khoi_file, 'r', encoding='utf-8') as f:
    khoi_lines = f.readlines()

with open(thuan_file, 'r', encoding='utf-8') as f:
    thuan_lines = f.readlines()

# Clean up khoi.sql (remove USE [master] and ALTER DATABASE at the end)
end_index = len(khoi_lines)
for i in range(len(khoi_lines)-1, -1, -1):
    if "USE [master]" in khoi_lines[i]:
        end_index = i
        break
khoi_cleaned = "".join(khoi_lines[:end_index])

tables_to_extract = ['[discounts]', '[premiums]', '[invoices]', '[user_subscriptions]']
extracted_content = "\n-- =========================================\n-- MERGED FROM thuan.sql\n-- =========================================\n"

# Extract CREATE TABLE blocks for the 4 tables
in_block = False
current_block = ""
for line in thuan_lines:
    if "CREATE TABLE" in line:
        for t in tables_to_extract:
            if t in line:
                in_block = True
                break
    
    if in_block:
        current_block += line
        if line.strip() == "GO":
            extracted_content += current_block + "\n"
            in_block = False
            current_block = ""

extracted_content += "-- DEFAULT VALUES\n"
# Extract DEFAULT VALUES for the 4 tables
in_block = False
for line in thuan_lines:
    if "ALTER TABLE" in line and ("ADD DEFAULT" in line or "ADD CONSTRAINT [DF_" in line):
        for t in tables_to_extract:
            if t in line:
                extracted_content += line
                in_block = True
                break
    elif in_block and line.strip() == "GO":
        extracted_content += line + "\n"
        in_block = False

extracted_content += "-- FOREIGN KEYS\n"
# Extract FOREIGN KEYS for the 4 tables
in_block = False
for line in thuan_lines:
    if "ALTER TABLE" in line and ("ADD CONSTRAINT [FK_" in line or "ADD CONSTRAINT [fk_" in line):
        if any(t in line for t in tables_to_extract):
            in_block = True
    
    if in_block:
        extracted_content += line
        if line.strip() == "GO":
            in_block = False
            
    # Also extract CHECK constraints for these tables if any
    if "ALTER TABLE" in line and "ADD CONSTRAINT [CHK_" in line:
        if any(t in line for t in tables_to_extract):
            in_block = True

# Extract basic auth roles
auth_roles_idx = -1
for i, line in enumerate(thuan_lines):
    if "BASIC AUTHENTICATION ROLES" in line:
        auth_roles_idx = i
        break

if auth_roles_idx != -1:
    extracted_content += "\n" + "".join(thuan_lines[auth_roles_idx:])

# Re-append the USE master stuff if necessary, or just leave it off since usually it's not needed for a DB script meant to run on an existing DB, but let's append it to be safe
extracted_content += "\n" + "".join(khoi_lines[end_index:])

with open(output_file, 'w', encoding='utf-8') as f:
    f.write(khoi_cleaned)
    f.write(extracted_content)
    
print(f"Merged successfully to {output_file}")
