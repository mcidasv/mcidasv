
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

public class GetExtensions extends JFrame implements GLEventListener {

    public GetExtensions() {
        super("OpenGL Extensions!");
        GLCapabilities caps = new GLCapabilities();
        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);
        add(canvas);
    }

    public void run() {
        setUndecorated(true);
        setSize(1, 1);
        setVisible(true);
    }

    // basically just the first point at which the appropriate strings can be
    // grabbed.
    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        System.out.println("GL_VENDOR: "+gl.glGetString(GL.GL_VENDOR));
        System.out.println("GL_VERSION: "+gl.glGetString(GL.GL_VERSION));
        System.out.println("GL_RENDERER: "+gl.glGetString(GL.GL_RENDERER));
        System.out.println("GL_EXTENSIONS: "+gl.glGetString(GL.GL_EXTENSIONS));
        runExit();
    }

    private static void runExit() {
        // Note: calling System.exit() synchronously inside the draw,
        // reshape or init callbacks can lead to deadlocks on certain
        // platforms (in particular, X11) because the JAWT's locking
        // routines cause a global AWT lock to be grabbed. Run the
        // exit routine in another thread.
        new Thread(new Runnable() {
            public void run() {
                System.exit(0);
            }
        }).start();
    }

    // needed by GLEventListener
    public void display(GLAutoDrawable d) {}
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {}
    public void displayChanged(GLAutoDrawable d, boolean m, boolean dc) {}

    public static void main(String[] args) {
        new GetExtensions().run();
    }
}
