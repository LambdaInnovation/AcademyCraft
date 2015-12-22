"""
 This script rebuilds tutorial contents to the lang of the given language.
 It is required to put this script under AcademyCraft\tutorials\ so that path lookup works correctly.
 Currently only supports windows. Other OS's contribution welcomed.
 @author: WeAthFolD
"""

import os
import sys
import re
import ctypes
from ctypes import cdll

is_64bits = sys.maxsize > 2**32

if is_64bits:
    md2lang = cdll.LoadLibrary('md2lang64.dll')
else:
    md2lang = cdll.LoadLibrary('md2lang32.dll')
    
md2lang.md2lang.argtypes = [ctypes.c_wchar_p]
md2lang.md2lang.restype = ctypes.c_wchar_p

wrong_usage_str = \
"""
Invalid arguments. Usage:

    python rebuild_lang.py <lang>
    
    This script rebuilds tutorial contents into the AC's lang file of the given language. It is required that the script be put under 'AcademyCraft\\tutorials\\' folder.
"""

_tutorial_prefix = "ac.gui.tutorial."
_title_postfix = "title"
_brief_postfix = "brief"
_content_postfix = "content"

_tutorial_det_regex = [re.compile(_tutorial_prefix.replace(".", "\.") + "[^\.]*\." + x) for x in [_title_postfix, _brief_postfix, _content_postfix]]

def is_tutorial_content(line):
    for x in _tutorial_det_regex:
        if x.match(line):
            return True
    return False
    
def build_kv(key, postfix, content):
    return _tutorial_prefix + key + '.' + postfix + '=' + md2lang.md2lang(content) + '\n'

def rebuild_lang(lang):
    lang_path = lang + '\\'
    
    if not os.path.isdir(lang_path):
        print("Can't find the path of given lang '%s'." % lang)
        return
    
    lang_file_path = '..\\src\\main\\resources\\assets\\academy\\lang\\%s.lang' % lang
    
    if not os.path.isfile(lang_file_path):
        print("Can't find the .lang file for given language.")
        return
    
    print('Reading file...')
    with open(lang_file_path, 'r', encoding='utf-8') as file:
        lines = file.readlines()
        
    print('Excluding previous tutorial contents...')
    lines = [ln for ln in lines if not is_tutorial_content(ln)]
    
    for file in os.listdir(lang_path):
        try:
            joined = os.path.join(lang_path, file)
            print('Processing ' + file + '...')
        
            lang_key = file[0:file.rindex('.txt')]
        
            with open(joined, "r", encoding="utf-8") as file:
                all_contents = file.read()
        
            ind_title = all_contents.index('![title]')
            ind_brief = all_contents.index('![brief]')
            ind_content = all_contents.index('![content]')
        
            # Ensure the index order is right
            assert ind_title < ind_brief and ind_brief < ind_content
        
            title = all_contents[ind_title+8:ind_brief].strip()
            brief = all_contents[ind_brief+8:ind_content].strip()
            content = all_contents[ind_content+10:].strip()
        
            lines.append(build_kv(lang_key, _title_postfix, title))
            lines.append(build_kv(lang_key, _brief_postfix, brief))
            lines.append(build_kv(lang_key, _content_postfix, content))
        except:
            print('Processing file %s failed. Please check your format specification.' % file)
        
    with open(lang_file_path, 'w', encoding='utf-8') as file:
        file.writelines(lines)
    
    
if __name__ == "__main__":
    args = sys.argv
    # print(args)
    if (len(args) != 2):
        print(wrong_usage_str)
    else:
        lang_name = args[1]
        rebuild_lang(lang_name)
		
		


