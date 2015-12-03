import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 * Benutzerinterface fuer eine Logo-aehnliche Sprache.
 */
public class Staffelei {
	private class DrawForward extends MoveForward {
		DrawForward(int length) {
			super(length);
		}

		@Override
		void replay(Graphics2D g, boolean draw, Rectangle bounds) {
			if (draw) {
				g.drawLine(0, 0, 0, this.length);
			}
			super.replay(g, draw, bounds);
		}
	}

	private class DrawTextLabel extends GraphicAction {
		private final String text;

		DrawTextLabel(String text) {
			this.text = text;
		}

		@Override
		void replay(Graphics2D g, boolean draw, Rectangle bounds) {
			AffineTransform origTrans = g.getTransform();
			Point2D center = origTrans
					.transform(new Point2D.Double(0, 0), null);

			FontRenderContext frc = g.getFontRenderContext();
			Font f = g.getFont();
			TextLayout tl = new TextLayout(text, f, frc);
			double w = tl.getBounds().getWidth();
			double h = tl.getBounds().getHeight();

			AffineTransform trans = new AffineTransform();
			trans.setToTranslation(Math.round(center.getX() - w / 2),
					Math.round(center.getY() + h / 2));

			g.setTransform(trans);
			Rectangle r = new Rectangle(0, (int) -h, (int) w, (int) h);

			if (draw) {
				Shape textShape = tl.getOutline(new AffineTransform());
				g.setColor(Color.BLACK);

				r.grow(4, 4);
				g.setColor(Color.WHITE);
				g.fill(r);
				g.setColor(Color.BLACK);
				g.draw(r);
				g.fill(textShape);
			}

			bounds.add(r);

			g.setTransform(origTrans);
		}
	}

	private abstract class GraphicAction {
		void addCurrentPos(Graphics2D g, Rectangle bounds) {
			if (bounds != null) {
				bounds.add(g.getTransform().transform(
						new Point2D.Float(0f, 0f), null));
			}
		}

		abstract void replay(Graphics2D g, boolean draw, Rectangle bounds);
	}

	private class MoveForward extends GraphicAction {
		final int length;

		MoveForward(int length) {
			this.length = length;
		}

		@Override
		void replay(Graphics2D g, boolean draw, Rectangle bounds) {
			addCurrentPos(g, bounds);
			AffineTransform t = g.getTransform();
			t.translate(0, this.length);
			g.setTransform(t);
			addCurrentPos(g, bounds);
		}
	}

	private class Pop extends GraphicAction {
		@Override
		void replay(Graphics2D g, boolean draw, Rectangle bounds) {
			g.setTransform(transformations.pop());
		}
	}

	private class Push extends GraphicAction {
		@Override
		void replay(Graphics2D g, boolean draw, Rectangle bounds) {
			transformations.push(g.getTransform());
		}
	}

	private class Rotate extends GraphicAction {
		final int degree;

		Rotate(int degree) {
			this.degree = degree;
		}

		@Override
		void replay(Graphics2D g, boolean draw, Rectangle bounds) {
			AffineTransform t = g.getTransform();
			t.rotate(this.degree / 180.0 * Math.PI);
			g.setTransform(t);
		}
	}

	protected Rectangle bounds;

	private ArrayList<Staffelei.GraphicAction> actions = new ArrayList<Staffelei.GraphicAction>();

	private final JComponent drawing = new JComponent() {
		@Override
		public void paintComponent(Graphics g) {
			synchronized (Staffelei.this) {
				Graphics2D g2 = (Graphics2D) g;

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);

				Rectangle clipBounds = g.getClipBounds();
				g2.setColor(Color.WHITE);
				g2.fill(clipBounds);
				g2.setColor(Color.BLACK);

				AffineTransform t = g2.getTransform();

				int drawCounter;

				// do we have current bounds?
				if (bounds == null) {
					Rectangle newBounds = new Rectangle();
					transformations.clear();
					drawCounter = 0;
					for (GraphicAction a : actions) {
						a.replay(g2, false, newBounds);
						if (a instanceof DrawForward
								|| a instanceof DrawTextLabel) {
							drawCounter++;
						}
					}
					bounds = new Rectangle(newBounds);
				}

				// move the drawing to the center of the screen
				t.setToTranslation(
						clipBounds.getWidth() / 2 - bounds.getCenterX(),
						clipBounds.getHeight() / 2 - bounds.getCenterY());

				// draw all lines
				g2.setTransform(t);
				transformations.clear();
				drawCounter = 0;
				for (GraphicAction a : actions) {
					boolean draw = !(a instanceof DrawTextLabel);
					a.replay(g2, draw, null);
					if (a instanceof DrawForward || a instanceof DrawTextLabel) {
						drawCounter++;
					}
					if (drawCounter >= renderMaxDraws) {
						break;
					}
				}

				// draw all labels
				g2.setTransform(t);
				transformations.clear();
				drawCounter = 0;
				for (GraphicAction a : actions) {
					boolean draw = (a instanceof DrawTextLabel);
					a.replay(g2, draw, null);
					if (a instanceof DrawForward || a instanceof DrawTextLabel) {
						drawCounter++;
					}
					if (drawCounter >= renderMaxDraws) {
						break;
					}
				}
			}
		}
	};

	private int draws;

	private final ActionListener handler = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "start":
				renderMaxDraws = 0;
				break;
			case "next":
				renderMaxDraws++;
				break;
			case "back":
				renderMaxDraws--;
				break;
			case "end":
				renderMaxDraws = draws;
				break;
			}
			drawing.repaint();
		}
	};

	private int renderMaxDraws = 1;

	private final Stack<AffineTransform> transformations = new Stack<>();

	@SuppressWarnings("unused")
	private final JFrame ui = new JFrame() {
		private final JButton jButton0;
		private final JButton jButton1;
		private final JButton jButton2;
		private final JButton jButton3;
		private final JPanel jPanel1;
		private final JScrollPane jScrollPane2;
		{
			jScrollPane2 = new JScrollPane(drawing);
			jPanel1 = new JPanel();
			jButton0 = new JButton("Anfang");
			jButton0.setActionCommand("start");
			jButton0.addActionListener(handler);
			jButton1 = new JButton("Zurueck");
			jButton1.setActionCommand("back");
			jButton1.addActionListener(handler);
			jButton2 = new JButton("Vor");
			jButton2.setActionCommand("next");
			jButton2.addActionListener(handler);
			jButton3 = new JButton("Ende");
			jButton3.setActionCommand("end");
			jButton3.addActionListener(handler);

			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
			jPanel1.setLayout(jPanel1Layout);
			jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(
					GroupLayout.Alignment.LEADING).addGap(0, 158,
					Short.MAX_VALUE));
			jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(
					GroupLayout.Alignment.LEADING).addGap(0, 36,
					Short.MAX_VALUE));

			GroupLayout layout = new GroupLayout(getContentPane());
			getContentPane().setLayout(layout);
			layout.setHorizontalGroup(layout
					.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(
							GroupLayout.Alignment.TRAILING,
							layout.createSequentialGroup()
									.addContainerGap()
									.addGroup(
											layout.createParallelGroup(
													GroupLayout.Alignment.TRAILING)
													.addComponent(
															jScrollPane2,
															GroupLayout.Alignment.LEADING,
															GroupLayout.DEFAULT_SIZE,
															458,
															Short.MAX_VALUE)
													.addGroup(
															GroupLayout.Alignment.LEADING,
															layout.createSequentialGroup()
																	.addComponent(
																			jButton0)
																	.addGap(6,
																			6,
																			6)
																	.addComponent(
																			jButton1)
																	.addGap(6,
																			6,
																			6)
																	.addComponent(
																			jButton2)
																	.addGap(6,
																			6,
																			6)
																	.addComponent(
																			jButton3)
																	.addPreferredGap(
																			LayoutStyle.ComponentPlacement.RELATED)
																	.addComponent(
																			jPanel1,
																			GroupLayout.DEFAULT_SIZE,
																			GroupLayout.DEFAULT_SIZE,
																			Short.MAX_VALUE)))
									.addContainerGap()));
			layout.setVerticalGroup(layout
					.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(
							GroupLayout.Alignment.TRAILING,
							layout.createSequentialGroup()
									.addContainerGap()
									.addComponent(jScrollPane2,
											GroupLayout.DEFAULT_SIZE, 188,
											Short.MAX_VALUE)
									.addPreferredGap(
											LayoutStyle.ComponentPlacement.RELATED)
									.addGroup(
											layout.createParallelGroup(
													GroupLayout.Alignment.TRAILING)
													.addComponent(
															jPanel1,
															GroupLayout.PREFERRED_SIZE,
															GroupLayout.DEFAULT_SIZE,
															GroupLayout.PREFERRED_SIZE)
													.addGroup(
															layout.createParallelGroup(
																	GroupLayout.Alignment.BASELINE)
																	.addComponent(
																			jButton0)
																	.addComponent(
																			jButton1)
																	.addComponent(
																			jButton2)
																	.addComponent(
																			jButton3)))
									.addContainerGap()));

			pack();

			this.setVisible(true);
		};
	};

	/**
	 * Malt eine Linie der Laenge {@code length} in die aktuelle Richtung.
	 * Hierbei wird auch die aktuelle Position an das Ende der Linie verschoben.
	 * 
	 * @param length
	 */
	public void drawForward(int length) {
		synchronized (this) {
			actions.add(new DrawForward(length));
			draws++;
			bounds = null;
		}
	}

	/**
	 * Malt ein Kaestchen mit dem Text {@code text} an die aktuelle Position.
	 * 
	 * @param text
	 */
	public void drawTextLabel(String text) {
		addAction(new DrawTextLabel(text));
	}

	private void addAction(GraphicAction action) {
		synchronized (this) {
			actions.add(action);
			if (action instanceof DrawForward
					|| action instanceof DrawTextLabel) {
				draws++;
			}
			bounds = null;
		}
	}

	/**
	 * Verschiebt die aktuelle Position um die Strecke {@code length} in die
	 * aktuelle Richtung.
	 * 
	 * @param length
	 */
	public void moveForward(int length) {
		addAction(new MoveForward(length));
	}

	/**
	 * Laedt eine gespeicherte Position und Ausrichtung. Siehe {@link #push()}.
	 */
	public void pop() {
		addAction(new Pop());
	}

	/**
	 * Speichert die aktuelle Postion und Ausrichtung.
	 * 
	 * Durch ein nachfolgendes {@link #pop()} wird die Position und Ausrichtung
	 * wiederhergestellt.
	 */
	public void push() {
		addAction(new Push());
	}

	/**
	 * Dreht die aktuelle Ausrichtung um {@code length} Grad (360 Grad bilden
	 * einen Kreis) im Uhrzeigersinn.
	 * 
	 * Negative Angaben von {@code length} die Ausrichtung gegen den
	 * Uhrzeigersinn.
	 * 
	 * @param length
	 */
	public void rotate(int length) {
		addAction(new Rotate(length));
	}
}
