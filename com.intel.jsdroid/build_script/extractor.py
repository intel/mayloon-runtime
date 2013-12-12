'''
Created on 2013-1-7

extract compiled class & javacript files from 

@author: luqiang
'''
#-*- coding: UTF-8 -*-

import os
import shutil
import sys

class ExtractorJSFiles(object):
    '''
    classdocs
    '''
    def __init__(self, srcFilePath, destFilePath):
        '''
        Constructor
        '''
        self.srcDir = srcFilePath
        self.destDir = destFilePath
        self.preparePath()
        
            
    def preparePath(self):
        removeJSDir = self.destDir+'/js/framework'
        if(os.path.exists(removeJSDir)) :
            shutil.rmtree(removeJSDir)
        removeClassDir = self.destDir+'/class/framework'
        if(os.path.exists(removeClassDir)) :
            shutil.rmtree(removeClassDir)
        
    def copyAllJsFiles(self):
        shutil.copytree(self.srcDir, self.destDir+'/js/framework', False, ignore=shutil.ignore_patterns('*.class'))
    
    def copyAllClassFiles(self):
        shutil.copytree(self.srcDir, self.destDir+'class/framework', False, ignore=shutil.ignore_patterns('*.js'))

if __name__ == "__main__":
#    extractor = ExtractorJSFiles('/home/luq/Dev/mayloon/mayloon-runtime/com.intel.jsdroid/bin/framework/', '/home/luq/Dev/mayloon_sdk/')
	current_path = os.path.abspath(os.path.dirname(sys.argv[0]))
	extractor = ExtractorJSFiles("../" + sys.argv[1], "../" + sys.argv[2])
	extractor.copyAllJsFiles()
#    extractor.copyAllClassFiles()
