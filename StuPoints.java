import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;

public class StuPoints extends Applet implements KeyListener, MouseListener, MouseMotionListener, Runnable{

	private Graphics dbGraphics;
	private Image dbImage;

	private Thread thread;

	private int clickX, clickY, dragX, dragY;
	private boolean mouseHold;
	private Point[] points;
	private Point[] midpoints;

	private CheckboxMenuItem[] colorSelections;
	private Menu menu;
	private MenuBar mb;

	public void init(){

		setSize(1000,700);
		setBackground(Color.BLACK);
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		points = new Point[3];
		midpoints = new Point[3];
		setPoints();

		//set up menu stuff
		colorSelections = new CheckboxMenuItem[3];
		colorSelections[0] = new CheckboxMenuItem("Red Distances", false);
		colorSelections[1] = new CheckboxMenuItem("Yellow Distances", false);
		colorSelections[2] = new CheckboxMenuItem("Green Distances", false);

		menu = new Menu("Display Distances (may be slightly off due to integer rounding)");
		for(CheckboxMenuItem c : colorSelections) menu.add(c);

		mb = new MenuBar();
		mb.add(menu);

		Object f = getParent();
		while (! (f instanceof Frame)) f = ((Component) f).getParent(); //go back until a parent is found that is instanceof Frame
		Frame frame = (Frame) f;
  		frame.setMenuBar(mb);
	}

	public void paint(Graphics g){

		g.setColor(Color.WHITE);

		for(int i=0; i<points.length; i++){ //plot points

			g.fillOval(points[i].x-7, points[i].y-7, 14, 14);
		}

		g.setColor(Color.LIGHT_GRAY);

		for(int i=0; i<midpoints.length; i++){ //plot midpoints

			g.fillOval(midpoints[i].x-4, midpoints[i].y-4, 8, 8);
		}

		for(int i=0; i<midpoints.length; i++){

			if(i == 0) g.setColor(Color.RED);
			if(i == 1) g.setColor(Color.YELLOW);
			if(i == 2) g.setColor(Color.GREEN);

			//java draws from top left so y coordinate is inverted in drawing y=mx+b

			int nextIndex; //2nd midpoint to draw a line through two midpoints

			if(i == midpoints.length-1) nextIndex = 0;
			else nextIndex = i+1;

			int cartesianY1 = getSize().height - midpoints[i].y;
			int cartesianY2 = getSize().height - midpoints[nextIndex].y;

			double cartesianM = ((double)(cartesianY2 - cartesianY1) / (double)(midpoints[nextIndex].x - midpoints[i].x)); //m =  y2-y1 / x2-x1, xs stay the same

			int cartesianB = (int)(cartesianY1 - (cartesianM * midpoints[i].x)); //b = y-mx
			int screenB = getSize().height - cartesianB; //invert b to draw on screen

			int xInt, xIntY;

			if(cartesianM < 0){ //low line xint

				xInt = (int)(-cartesianB / cartesianM);
				xIntY = getSize().height;
			}

			else if(cartesianM > 0){ //high line xint

				xInt = (int)((getSize().height - cartesianB) / cartesianM);
				xIntY = 0;
			}

			else{

				xInt = (int)(-cartesianB / cartesianM);
				xIntY = (int)(getSize().height / 2.0);
			}

			g.drawLine(0, screenB, xInt, xIntY);

			//distances are slightly off due to rounding, and java only paints in ints

			if((i == 0 && colorSelections[0].getState()) || (i == 1 && colorSelections[1].getState()) || (i == 2 && colorSelections[2].getState())){ //show perpendiculars

				for(int j=0; j<points.length; j++){

					double perpCartesianM = -1/cartesianM;
					int perpCartesianB = (int)((getSize().height - points[j].y) - (perpCartesianM * points[j].x)); //b = y-mx

					int xIntersection = (int)((cartesianB - perpCartesianB) / (perpCartesianM - cartesianM)); // x = b0-b1 / m1-m0
					int yIntersection = getSize().height - (int)((cartesianM * xIntersection) +  cartesianB);

					g.drawLine(points[j].x, points[j].y, xIntersection, yIntersection);
					g.drawString("d = " + (int)(Math.sqrt(Math.pow(yIntersection-points[j].y,2) + Math.pow(xIntersection-points[j].x,2))), (points[j].x+xIntersection)/2, (points[j].y+yIntersection)/2);
				}
			}
		}

		if(mouseHold){

			g.setColor(Color.WHITE);
			g.drawLine(clickX, clickY, dragX, dragY);
			int distance = (int)(Math.sqrt(Math.pow(dragY-clickY,2) + Math.pow(dragX-clickX,2)));
			g.drawString("Distance: " + distance, dragX, dragY-20);
		}
	}

	public void update(Graphics g){

		if(dbImage == null){

			dbImage = createImage(getSize().width, getSize().height);
			dbGraphics = dbImage.getGraphics();
		}

		dbGraphics.setColor(getBackground());
		dbGraphics.fillRect(0, 0, getSize().width, getSize().height);
		dbGraphics.setColor(getForeground());
		paint(dbGraphics);

		g.drawImage(dbImage, 0, 0, this);
	}

	public void setPoints(){

		for(int i=0; i<points.length; i++){ //generate three random points

			int rx;
			int ry;

			boolean repeat = false;

			do{  //make sure no points are the same

				rx = (int)(getSize().width * Math.random());
				ry = (int)(getSize().height * Math.random());

				repeat = false;

				for(int j=0; j<i; j++){

					if(points[j].x == rx && points[j].y == ry) repeat = true;

					if(i == points.length-1){ //make sure 3 points arent collinear

						double slope = (double)(points[1].y - points[0].y) / (double)(points[1].x - points[0].x);
						double b = points[0].y - (slope * points[0].x); //b = y-mx

						if(ry == (slope)*(rx) + b) repeat = true;
					}
				}
			}while(repeat);

			points[i] = new Point(rx, ry);
		}

		for(int i=0; i<midpoints.length; i++){

			int nextIndex;

			if(i == midpoints.length-1) nextIndex = 0;
			else nextIndex = i+1;

			midpoints[i] = new Point((int)((points[i].x + points[nextIndex].x)/2.0), (int)((points[i].y + points[nextIndex].y)/2.0));
		}
	}

	public void mouseEntered(MouseEvent e){
	}

	public void mouseExited(MouseEvent e){
	}

	public void mouseMoved(MouseEvent e){
	}

	public void mouseDragged(MouseEvent e){

		mouseHold = true;
		dragX = e.getX();
		dragY = e.getY();
	}

	public void mousePressed(MouseEvent e){

		mouseHold = false;
		clickX = e.getX();
		clickY = e.getY();
	}

	public void mouseReleased(MouseEvent e){
	}

	public void mouseClicked(MouseEvent e){
	}

	public void keyPressed(KeyEvent e){
	}

	public void keyReleased(KeyEvent e){
	}

	public void keyTyped(KeyEvent e){

		int keyCode = e.getKeyCode();

		if(keyCode == 0) setPoints(); //hmm, there seems to be no focus... its always 0 no matter the keytyped try putting this in a diffrent method - keypressed
	}

	public void start(){

		if(thread == null){

			thread = new Thread(this);
			thread.start();
		}
	}

	public void run(){

		while(thread != null){

			repaint();

			try{

				Thread.sleep(20);
			}
			catch(InterruptedException e){
			}
		}
	}

	public void stop(){

		thread = null;
	}

	public static void main(String[] args){

		JFrame frame = new JFrame("StuPoints");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setLayout( new BorderLayout() );

		Applet thisApplet = new StuPoints();

		frame.getContentPane().add( thisApplet, BorderLayout.CENTER );
		thisApplet.init();
		frame.setSize( thisApplet.getSize() );
		thisApplet.start();
		frame.setVisible(true);
	}
}
