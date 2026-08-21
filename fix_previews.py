import os
import re

directory = 'app/src/main/java/com/example/staybuddy/ui/screens'

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find all preview function definitions
            matches = list(re.finditer(r'@Composable\s*\n\s*fun\s+([A-Za-z0-9_]+Preview)\s*\(', content))
            if len(matches) > 1:
                # Group by function name
                names = {}
                for m in matches:
                    names[m.group(1)] = names.get(m.group(1), 0) + 1
                
                for name, count in names.items():
                    if count > 1:
                        # Find the last occurrence of this function and its preceding @Preview
                        # We will just split the file and remove the last one.
                        # Since they were appended at the end, we can look for the last occurrence of the function name.
                        print(f"Fixing {file}: {name} appears {count} times")
                        
                        # Find the last @androidx.compose.ui.tooling.preview.Preview... block
                        # that defines this function
                        pattern = r'@(?:androidx\.compose\.ui\.tooling\.preview\.)?Preview[^\n]*\n\s*@Composable\s*\n\s*fun\s+' + name + r'\s*\([^)]*\)\s*\{'
                        last_match = list(re.finditer(pattern, content))[-1]
                        
                        start_idx = last_match.start()
                        
                        # Find matching closing brace
                        brace_count = 0
                        in_block = False
                        end_idx = start_idx
                        for i in range(start_idx, len(content)):
                            if content[i] == '{':
                                brace_count += 1
                                in_block = True
                            elif content[i] == '}':
                                brace_count -= 1
                            
                            if in_block and brace_count == 0:
                                end_idx = i + 1
                                break
                                
                        # Remove the block
                        content = content[:start_idx] + content[end_idx:]
                        
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
