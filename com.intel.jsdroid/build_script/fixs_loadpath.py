#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
#  fixs_loadpath.py
#  


import os
import sys
import simplejson

def ls(dir, hidden=False, relative=True):
    nodes = []
    for nm in os.listdir(dir):
        if not hidden and nm.startswith('.'):
            continue
        if not relative:
            nm = os.path.join(dir, nm)
        nodes.append(nm)
    nodes.sort()
    return nodes

def find(root, files=True, dirs=False, hidden=False, relative=True, topdown=True):
    root = os.path.join(root, '')  # add slash if not there
    for parent, ldirs, lfiles in os.walk(root, topdown=topdown):
        if relative:
            parent = parent[len(root):]
        if dirs and parent:
            yield os.path.join(parent, '')
        if not hidden:
            lfiles = [nm for nm in lfiles if not nm.startswith('.')]
            ldirs[:] = [nm for nm in ldirs  if not nm.startswith('.')]  # in place
        if files:
            lfiles.sort()
            for nm in lfiles:
                nm = os.path.join(parent, nm)
                yield nm

def fixs_loadpath(root):
    all_list = []
    package_set = set()
    
    current_path = os.path.abspath(os.path.dirname(sys.argv[0]))
    
    print("java output path: %s" % "../" + root)
    print("current execution script path: %s" % current_path)
    for f in find("../" + root):
        all_list.append(f)
    
    for string in all_list:
        index = str(string).rfind("/")
        temp = str(string)[0:index]
        temp = temp.replace("/", ".")
        package_set.add(temp)
    
    package_list = list(package_set)
    write_file = open('../sdk_config/package_name.json', 'w')
    simplejson.dump(package_list, write_file)
    write_file.close()
    
    print(package_list)
                

if __name__ == "__main__":
    fixs_loadpath(sys.argv[1])
