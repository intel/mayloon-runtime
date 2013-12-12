package android.opengl.OpenGLES10;

public class StateShaderProgram
{
    public int[] state = null;
    public ShaderProgram shaderProgram;
    
	public StateShaderProgram(int[] state, ShaderProgram program)
	{
		this.state = state;
		this.shaderProgram = program;
	}

}