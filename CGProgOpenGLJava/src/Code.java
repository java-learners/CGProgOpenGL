import static com.jogamp.opengl.GL.GL_NO_ERROR;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_COMPILE_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_INFO_LOG_LENGTH;
import static com.jogamp.opengl.GL2ES2.GL_LINK_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;

public class Code extends JFrame implements GLEventListener {
	private static final long serialVersionUID = 1L;

	private GLCanvas myCanvas;

	private int rendering_program;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];

	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private GLSLUtils util = new GLSLUtils();
	private Matrix3D pMat;

	public Code() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("CG Programming in OpenGL with Java");
		int x = getConfigInt("windowX");
		int y = getConfigInt("windowY");
		System.out.printf("window: (%d, %d)\n", x, y);
		setLocation(x, y);
		setSize(600, 600);

		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		setVisible(true);
	}

	public void init( GLAutoDrawable drawable ) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		rendering_program = createShaderProgram();
		setupVertices();
		cameraX = 0.0f;
		cameraY = 0.0f;
		cameraZ = 8.0f;
		cubeLocX = 0.0f;
		cubeLocY = -2.0f;
		cubeLocZ = 0.0f;

		// Create a perspective matrix. This one has fovy=60, aspect ratio matches
		// screen window. Values for near and far clipping planes can vary
		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat = perspective(60.0f, aspect, 0.1f, 1000.0f);

		FPSAnimator animator = new FPSAnimator(drawable, 60);
		animator.start();
	}

	public void display( GLAutoDrawable drawable ) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL4.GL_COLOR, 0, bkgBuffer);
		gl.glUseProgram(rendering_program);

		double timeFactor = (double) (System.currentTimeMillis() % 3600000) / 10000.0;

		// build model and view matrices
		Matrix3D mMat = new Matrix3D();
		Matrix3D vMat = new Matrix3D();

		// Adjust camera
		vMat.translate(-cameraX, -cameraY, -cameraZ);

		// Attach uniform variables
		int m_loc = gl.glGetUniformLocation(rendering_program, "m_matrix");
		int v_loc = gl.glGetUniformLocation(rendering_program, "v_matrix");
		int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		int tf_loc = gl.glGetUniformLocation(rendering_program, "tf");
		int sf_loc = gl.glGetUniformLocation(rendering_program, "speedFactor");
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(m_loc, 1, false, mMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(v_loc, 1, false, vMat.getFloatValues(), 0);
		gl.glUniform1f(tf_loc, (float) timeFactor);
		gl.glUniform1f(sf_loc, (float) getSpeedFactor());

		// associate VBO with the corresponding vertex attribute in the vertex shader
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// adjust OpenGL settings and draw model
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDrawArraysInstanced(GL_TRIANGLES, 0, 36, getConfigInt("numCubes"));
	}

	private void setupVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		// 36 vertices of the 12 triangles making up a 2x2x2 cube centered at the origin
		float[] vertex_positions = { -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,

				1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,

				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,

				1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,

				1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,

				-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,

				-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f,

				-1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,

				-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f,

				1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,

				-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,

				1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f };
//    	findDuplicateVerts(vertex_positions);
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(vertex_positions);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL.GL_STATIC_DRAW);
	}

	public static void main( String[] args ) {
		new Code();
	}

	public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) {
	}

	public void dispose( GLAutoDrawable drawable ) {
	}

	private int createShaderProgram() {

		GL4 gl = (GL4) GLContext.getCurrentGL();

		int[] vertCompiled = new int[1];
		int[] fragCompiled = new int[1];
		int[] linked = new int[1];

		String[] vshaderSource;
		String[] fshaderSource;

		vshaderSource = readShaderSource("Shaders/vert.glsl");
		fshaderSource = readShaderSource("Shaders/frag.glsl");

		// Compile vertex shader
		int vertexShader = gl.glCreateShader(GL_VERTEX_SHADER);
		gl.glShaderSource(vertexShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glCompileShader(vertexShader);
		checkOpenGLError();
		gl.glGetShaderiv(vertexShader, GL_COMPILE_STATUS, vertCompiled, 0);
		if ( vertCompiled[0] == 1 ) {
			System.out.println("... vertex compilation success.");
		} else {
			System.out.println("... vertex compilation failed.");
			printShaderLog(vertexShader);
		}

		// Compile fragment shader
		int fragmentShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		gl.glShaderSource(fragmentShader, fshaderSource.length, fshaderSource, null, 0);
		gl.glCompileShader(fragmentShader);
		checkOpenGLError();
		gl.glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, fragCompiled, 0);
		if ( fragCompiled[0] == 1 ) {
			System.out.println("... fragment compilation success.");
		} else {
			System.out.println("... fragment compilation failed.");
			printShaderLog(fragmentShader);
		}

		if ( (vertCompiled[0] != 1) || (fragCompiled[0] != 1) ) {
			System.out.println("\nCompilation error; return-flags:");
			System.out.println(" vertCompiled = " + vertCompiled[0] + "; fragCompiled = " + fragCompiled[0]);
		} else {
			System.out.println("Successful compilation");
		}

		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vertexShader);
		gl.glAttachShader(vfprogram, fragmentShader);
		gl.glLinkProgram(vfprogram);
		checkOpenGLError();
		gl.glGetProgramiv(vfprogram, GL_LINK_STATUS, linked, 0);
		if ( linked[0] == 1 ) {
			System.out.println("... linking succeeded.");
		} else {
			System.out.println("... linking failed.");
		}

		gl.glDeleteShader(vertexShader);
		gl.glDeleteShader(fragmentShader);

		return vfprogram;
	}

	private String[] readShaderSource( String filename ) {
		Vector<String> lines = new Vector<String>();
		Scanner sc = null;

		try {
			File shaderSourceFile = new File(filename);
//            System.out.println("Path: " + shaderSourceFile.getAbsolutePath());
			sc = new Scanner(shaderSourceFile);
		} catch ( IOException e ) {
			System.err.println("IOException reading file: " + e);
			return null;
		}

		while ( sc.hasNext() ) {
			lines.addElement(sc.nextLine());
		}
		sc.close();

		String[] program = new String[lines.size()];
		for ( int i = 0; i < lines.size(); ++i ) {
			program[i] = (String) lines.elementAt(i) + "\n";
		}

		return program;
	}

	private void printShaderLog( int shader ) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		int[] len = new int[1];
		int[] chWritten = new int[1];
		byte[] log = null;

		// determine length of the shader compilation log
		gl.glGetShaderiv(shader, GL_INFO_LOG_LENGTH, len, 0);
		if ( len[0] > 0 ) {
			log = new byte[len[0]];
			gl.glGetShaderInfoLog(shader, len[0], chWritten, 0, log, 0);
			System.out.println("Shader Info Log: ");
			for ( int i = 0; i < log.length; ++i ) {
				System.out.print((char) log[i]);
			}
		}
	}

	@SuppressWarnings("unused")
	private void printProgramLog( int prog ) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		int[] len = new int[1];
		int[] chWritten = new int[1];
		byte[] log = null;

		// determine length of the program linking log
		gl.glGetProgramiv(prog, GL_INFO_LOG_LENGTH, len, 0);
		if ( len[0] > 0 ) {
			log = new byte[len[0]];
			gl.glGetProgramInfoLog(prog, len[0], chWritten, 0, log, 0);
			System.out.println("Program Info Log: ");
			for ( int i = 0; i < log.length; ++i ) {
				System.out.print((char) log[i]);
			}
		}
	}

	private boolean checkOpenGLError() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		boolean foundError = false;
		GLU glu = new GLU();
		int glErr = gl.glGetError();
		while ( glErr != GL_NO_ERROR ) {
			System.err.println("glError: " + glu.gluErrorString(glErr));
			foundError = true;
			glErr = gl.glGetError();
		}

		return foundError;
	}

	private Matrix3D perspective( float fovy, float aspect, float n, float f ) {
		float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0, 0, A);
		r.setElementAt(1, 1, q);
		r.setElementAt(2, 2, B);
		r.setElementAt(3, 2, -1.0f);
		r.setElementAt(2, 3, C);
		r.setElementAt(3, 3, 0.0f);
		return r;
	}

	//////////////
	// DEBUGGING:
	//////////////
	class Vector3 {
		float x, y, z;

		public Vector3( float x, float y, float z ) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public boolean equals( Object obj ) {
			if ( obj == null )
				return false;
			if ( obj == this )
				return true;
			if ( obj instanceof Vector3 ) {
				Vector3 other = (Vector3) obj;
				return (this.x == other.x && this.y == other.y && this.z == other.z);
			}
			return false;
		}

		@Override
		public String toString() {
			return String.format("%.1f, %.1f, %.1f", x, y, z);
		}
	}

	private void findDuplicateVerts( float[] vertArray ) {
		System.out.println(vertArray.length);
		if ( vertArray.length % 3 != 0 )
			throw new RuntimeException("vertex array should be divisible by 3");

		Map<String, Integer> vertMap = new Hashtable<String, Integer>();
		for ( int i = 0; i < vertArray.length - 2; i += 3 ) {
			String vecString = new Vector3(vertArray[i], vertArray[i + 1], vertArray[i + 2]).toString();
			if ( !vertMap.containsKey(vecString) )
				vertMap.put(vecString, 1);
			else
				vertMap.put(vecString, vertMap.get(vecString) + 1);
		}
		for ( String vert : vertMap.keySet() ) {
			System.out.printf("[%s]: %d\n", vert, vertMap.get(vert));
		}
	}

	/**
	 * Load "slowFactor" variable from config file (default: 1.0)
	 * 
	 * @return "slowFactor" variable from config file; 1.0 if config file is not
	 *         found.
	 */
	private double getSpeedFactor() {
		final String configFilename = "./debug.cfg";

		File cfgFile = new File(configFilename);
//    	if (!cfgFile.exists()) return 1.0;

		try ( Scanner cfgScanner = new Scanner(cfgFile) ) {
			while ( cfgScanner.hasNextLine() ) {
				String line = cfgScanner.nextLine();
				if ( line.startsWith("speedFactor") && line.contains("=") ) {
					// handle comments
					String[] tokens = line.split("#")[0].split("=");
					double slowFactor = 1.0;
					try {
						slowFactor = Double.parseDouble(tokens[1].trim());
					} catch ( NumberFormatException e ) {
						System.out.println("Invalid config format. Using default value.");
						slowFactor = 1.0;
					}
//    				System.out.println("slowFactor: " + slowFactor);
					return slowFactor;
				}
			}
		} catch ( FileNotFoundException e ) {
			System.out.println("File " + cfgFile.getAbsolutePath() + " not found. Returning 1.0");
			return 1.0;
		}
		return 1.0;
	}

	private int getConfigInt( String key ) {
		final String configFilename = "./debug.cfg";

		File cfgFile = new File(configFilename);
//    	if (!cfgFile.exists()) return 1.0;

		try ( Scanner cfgScanner = new Scanner(cfgFile) ) {
			while ( cfgScanner.hasNextLine() ) {
				String line = cfgScanner.nextLine();
				if ( line.startsWith("#") )
					continue;
				if ( line.trim().startsWith(key) && line.contains("=") ) {
					String[] tokens = line.split("=");
					int value = 0;
					try {
						value = Integer.parseInt(tokens[1].trim());
					} catch ( NumberFormatException e ) {
						System.out.println("Invalid config format for \"" + key + "\". Using default value.");
						return 0;
					}
					return value;
				}
			}
		} catch ( FileNotFoundException e ) {
			System.out.println("File " + cfgFile.getAbsolutePath() + " not found. Returning 1.0");
			return 0;
		}
		return 0;
	}
}
