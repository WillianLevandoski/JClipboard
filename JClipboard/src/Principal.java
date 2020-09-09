import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class Principal implements NativeKeyListener {

	public static List<String> historico = new ArrayList<String>();
	private static String nomeArquivo = "jclipboard.txt";
	private static String pesquisa = "";

	public static void main(String[] args) {
		try {
			GlobalScreen.registerNativeHook();
			desativarLog();
		} catch (NativeHookException e) {
			System.exit(1);
		}
		GlobalScreen.addNativeKeyListener(new Principal());
		criarLerArquivo();
	}

	private static void desativarLog() {
		LogManager.getLogManager().reset();
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
	}

	@Override
	public void nativeKeyPressed(NativeKeyEvent event) {
		if (presionadoCopia(event)) {
			esperar(event);
			trataCopia();
		} else if (presionadoColarEspecial(event)) {
			imprimir();
		}else if(presionadoSair(event)) {
			System.exit(1);
		}
	}

	private void esperar(NativeKeyEvent event) {
		synchronized (event) {
			try {
				event.wait(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void trataCopia() {
		if (historico.isEmpty())
			historico.add(getClipBoard());
		else
			trataAddCopia();
	}

	private void trataAddCopia() {
		if (historico.contains(getClipBoard())) {
			historico.remove(getClipBoard());
			escreverArquivo();
		} else {
			escreverArquivo();
		}
		historico.add(getClipBoard());
	}

	private void escreverArquivo() {
		try {
			Writer myWriter = new FileWriter(getPath() + "\\" + nomeArquivo);
			myWriter.append(getClipBoard());
			for (String str : historico) {
				myWriter.append("\r\n");
				myWriter.append(str);
			}
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void imprimir() {
		JFrame frame = new JFrame();
		autoclose(frame);
		frame.getRootPane().setBorder(BorderFactory.createMatteBorder(4, 4, 4, 4, Color.red));
		frame.setUndecorated(true);
		populaFrame(frame);
		frame.pack();
		definePosicao(frame);
		toFront(frame);
	}

	private void definePosicao(JFrame frame) {
		Point p = MouseInfo.getPointerInfo().getLocation();
		frame.setLocation((int) p.getX(), (int) p.getY());
	}
	private void populaFrame(JFrame frame) {
		JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pane.setLayout(new GridLayout(0, 1));
		pane.setMaximumSize(new Dimension(30, 80));
		addTextField(pane);
		addBotao(frame, pane);
		frame.add(pane);
	}

	private void addTextField(JPanel pane) {
		JTextField textFiled = new JTextField("Pesquisa", 1);
		textFiled.setToolTipText("Pesquisa");
		limpaCampo(textFiled);
		filtro(textFiled);
		pane.add(textFiled);
	}

	private void filtro(JTextField textFiled) {
		textFiled.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER) {
					imprimir();
				} else {
					pesquisa = pesquisa + e.getKeyChar();
				}
			}

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}
		});
	}

	private void limpaCampo(JTextField textFiled) {
		textFiled.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				textFiled.setText(null);
				pesquisa = "";
			}

			public void focusLost(FocusEvent e) {
			}
		});
	}

	private void addBotao(JFrame frame, JPanel pane) {
		int count = 1;
		List<String> historivoInvertido = historico;
		Collections.reverse(historivoInvertido);
		for (String str : historivoInvertido) {
			if (str.length() > 0 && str.contains(pesquisa)) {
				String texto = str;
				if (str.length() > 80)
					texto = str.substring(0, 80);
				JButton button = new JButton(count + " - " + texto);
				count++;
				setAlinhamentoBorda(button);
				trataDuploClique(frame, str, button);
				if(count<=21)
					pane.add(button, BorderLayout.EAST);
			}
		}
		pesquisa = "";
	}
	private void trataDuploClique(JFrame frame, String str, JButton button) {
		button.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent me) {
				if (me.getClickCount() == 2) {
					setClipBoard(str);
					frame.setVisible(false);
				}
			}
		});
	}

	private void setAlinhamentoBorda(JButton button) {
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setContentAreaFilled(true);
		button.setFocusPainted(true);
		button.setBorder(new LineBorder(Color.DARK_GRAY));
		button.setContentAreaFilled(false);
	}

	public void toFront(JFrame frame) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				frame.setVisible(true);
				int state = frame.getExtendedState();
				frame.setExtendedState(state);
				frame.setAlwaysOnTop(true);
				frame.toFront();
				frame.requestFocus();
				frame.setAlwaysOnTop(false);
			}
		});
	}

	public void autoclose(JFrame frame) {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addVetoableChangeListener("focusedWindow",
				new VetoableChangeListener() {
					private boolean gained = false;

					@Override
					public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
						if (evt.getNewValue() == frame) {
							gained = true;
						}
						if (gained && evt.getNewValue() != frame) {
							frame.dispose();
						}
					}
				});
	}

	private boolean presionadoCopia(NativeKeyEvent event) {
		int teclaCtrl = event.CTRL_MASK;
		int teclaC = event.VC_C;
		if ((event.getKeyCode() == teclaC) && ((event.getModifiers() & teclaCtrl) != 0))
			return true;
		return false;
	}

	private boolean presionadoColarEspecial(NativeKeyEvent event) {
		int teclaMeta = event.META_L_MASK;
		int teclaV = event.VC_V;
		if ((event.getKeyCode() == teclaV) && ((event.getModifiers() & teclaMeta) != 0))
			return true;
		return false;
	}
	

	private boolean presionadoSair(NativeKeyEvent event) {
		int teclaMeta = event.META_L_MASK;
		int teclaO = event.VC_O;
		if ((event.getKeyCode() == teclaO) && ((event.getModifiers() & teclaMeta) != 0))
			return true;
		return false;
	}

	@Override
	public void nativeKeyReleased(NativeKeyEvent arg0) {

	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	public static String getClipBoard() {
		String result = "";
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		boolean hasStringText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasStringText) {
			try {
				result = (String) contents.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException | IOException ex) {
				System.out.println(ex);
				ex.printStackTrace();
			}
		}
		return result;
	}

	public static void setClipBoard(String str) {
		StringSelection selection = new StringSelection(str);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}

	public static void criarLerArquivo() {
		try {
			String path = getPath();
			File folder = new File(path);
			folder.mkdir(); // create a folder in your current work space
			File file = new File(folder, nomeArquivo); // put the file inside the folder
			file.createNewFile();
			popularHistorico(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void popularHistorico(File myObj) {
		try {
			Scanner myReader = new Scanner(myObj);
			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				historico.add(data);
			}
			myReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static String getPath() {
		String path = System.getProperty("user.home");
		return path;
	}

}