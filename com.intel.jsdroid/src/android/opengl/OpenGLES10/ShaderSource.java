package android.opengl.OpenGLES10;

/*
 Copyright 2009 Johannes Vuorinen
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at 
 
 http://www.apache.org/licenses/LICENSE-2.0 
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */



public class ShaderSource
{
    private ShaderFile file;
    private String additionalSource;
    private String source;
    private boolean sourceExpanded;
    
    public ShaderSource(ShaderFile file)
    {
        this.file = file;
        this.additionalSource = new String();
        this.sourceExpanded = false;

    }
    
    public ShaderSource(ShaderFile file, String additionalSource)
    {
        this.file = file;
        this.additionalSource = additionalSource;
        this.sourceExpanded = false;

    }

    public void appendAdditionalSource(String newAdditionalSource)
    {
        additionalSource += newAdditionalSource;
        sourceExpanded = false;
    }
    
    public ShaderFile getFile()
    {
        return file;
    }
    
    public String getSource()
    {
        if (!sourceExpanded)
        {
            expandSource();
        }
        return source;
    }

    private boolean expandSource()
    {
        /*if (!file.open())
        {
            (...) if (GlobalMembersOpenGLESConfig.DEBUG) {OpenGLESUtil.logMessage(__VA_ARGS__);}(new OpenGLESString("ERROR: Cannot open file ") + file.getName());
            return false;
        }
        file.seek(0, SEEK_END);
        int pos = file.tell();
        file.seek(0, SEEK_SET);

        int n = file.read(expandSource_tmp, 1, pos);
        expandSource_tmp = tangible.StringFunctions.changeCharacter(expandSource_tmp, n, '\0');
        file.close();

        int additionalSourceLength = additionalSource.length();
        byte sourceTmp = (String)malloc(sizeof(byte) * n + additionalSourceLength + 1);
        if (sourceTmp == null)
        {
            (...) if (GlobalMembersOpenGLESConfig.DEBUG) {OpenGLESUtil.logMessage(__VA_ARGS__);}(__FILE__, __LINE__, "ERROR: Cannot allocate memory.");
            return false;
        }

        sourceTmp = additionalSource;
        sourceTmp + additionalSourceLength = expandSource_tmp.substring(0, n + 1);

        source = sourceTmp;
        sourceExpanded = true;*/

        return true;
    }
}
