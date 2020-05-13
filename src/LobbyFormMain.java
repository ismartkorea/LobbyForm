import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.hibiznet.libworks.GameClientListener;
import net.hibiznet.libworks.GameCommunicator;
import net.hibiznet.libworks.LobbyForm;


public class LobbyFormMain extends JFrame implements GameClientListener,
		ActionListener {

	private static final long serialVersionUID = 1L;

	// 엔트리 포인트
	public static void main(String[] args) {
		new LobbyFormMain();
	}

	// 통신관련 필드
	private static final String GAMEIDPD = "12345678-1234-1234-1234-123456789ABC|password";
	private static final String SERVERHOSTNAME = "localhost";
	private static final int WAITPORT = 15008;
	GameCommunicator communicator = null;
	private static final int UCOM_SENDMESSAGE = 1000;

	// 텍스트 박스 및 버튼
	JTextField tf_message = new JTextField(20);
	JTextArea ta_result = new JTextArea();
	JButton btn_send = new JButton("몭륪");

	// 로비 폼.
	LobbyForm lobbyform = null;

	// 컨스트럭트
	public LobbyFormMain() {
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// 클로즈 시 처리 : 서버로부터 끊김.
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				// disconnect 메소드 호출.
				if (communicator != null)
					communicator.disconnect();
			}
		});

		// 윈도우 초기화 처리.
		this.setTitle("챗트");
		Container cPane = this.getContentPane();
		// 입력 부분
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.add(this.tf_message);
		panel1.add(this.btn_send);
		this.btn_send.setEnabled(false);
		cPane.add(panel1, BorderLayout.NORTH);
		// 메시지 부분
		this.ta_result.setRows(8);
		this.ta_result.setEditable(false);
		JScrollPane scpane = new JScrollPane(this.ta_result);
		cPane.add(scpane, BorderLayout.CENTER);
		// 송신버튼에 액션 할당
		this.btn_send.addActionListener(this);
		// 디폴트 버튼을 (Enter키로 누르는 버튼) 설정.
		this.getRootPane().setDefaultButton(this.btn_send);

		this.pack(); // 윈도우 사이즈 자동 조정.
		this.setVisible(true);
		this.setLocationRelativeTo(null); // 데스크탑중앙에 배치.

		// 서버에 접속하고 게임에 로그인
		this.communicator = new GameCommunicator();
		this.communicator.connectServer(SERVERHOSTNAME, WAITPORT, GAMEIDPD);

		// 로그인 확인.
		if (this.communicator.isLogin() == true) {
			// 유저 명 설정 다이어로그 표시.
			String username = null;
			while (username == null) {
				username = JOptionPane.showInputDialog(this,
						"자신의 닉네임을 입력하세요.");
			}

			// 자신의 유저명을 등록.
			this.communicator.sendToServer(GameCommunicator.COM_SETUSERNAME, 0,
					0, username);
			this.setTitle("챗트: " + username);
			// 로비 폼 다이어로그 표시.
			this.lobbyform = new LobbyForm(this, this.communicator, GAMEIDPD);
			this.lobbyform.setUsername(username);
			this.lobbyform.setMultiplay(false);
			this.lobbyform.showRobby();
			// 모달 다이어로그로 하면 수신 타이머가 정지하기 때문에
			//  모달하기 위해 로프 
			while (this.lobbyform.isVisible()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("모달 해제");
			// 대전 준비가 준비되면 송신 버튼을 유효로 함.
			if (this.lobbyform.getSucceed() == true) {
				this.communicator.setGameClientListener(this);
				this.btn_send.setEnabled(true);
			}
		} else {
			System.out.println("어떤 이유로 로그인 실패");
			JOptionPane.showMessageDialog(null, "서버 접속 또는 로그인에 실패했습니다.\n"
					+ "ID, 패스워드, 호스트명, 포트가 맞는 지 \n"
					+ "서버가 다운된 가능성이 있습니다.", "접속 에러",
					JOptionPane.WARNING_MESSAGE);

		}

	}

	// 메시지 수신 시 호출되는 메소드
	@Override
	public void receive(int command, int startpt, int endpt, String body) {
		if (command == UCOM_SENDMESSAGE) {
			// 상대로부터 메시지을 수신
			// 수신한 메시지를 표시.
			this.ta_result.append(this.lobbyform.getOpponentName() + ": "
					+ body + "\n");
			// 스크롤 위치 조정.
			this.ta_result.setCaretPosition(ta_result.getText().length());
		} else if (command == 0) {
			// 에러 발생
			this.ta_result.append("송신한 메시지은 상대에게 수신되지 않았습니다.\n");
			// 스크롤 위치 조정.
			this.ta_result.setCaretPosition(ta_result.getText().length());

		}
	}

	// 수신 에러가 발생할 때 불러지는 메소드
	// 필요하지 않으면 아무것도 안해도 좋음.
	@Override
	public void receiveError() {
	}

	// Communicator 수신용 타이머을 이용한 정기 이벤트을 호출.
	@Override
	public void receiveTimerRun() {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// 송신버튼을 눌렀을 때
		if (e.getSource() == this.btn_send) {
			// 문자수을 확인.
			String sendmessage = this.tf_message.getText();
			if (sendmessage.length() > 0) {
				// 메시지 송신
				this.communicator.sendToServer(UCOM_SENDMESSAGE, this.lobbyform
						.getUserID(), this.lobbyform.getOpponentID(),
						sendmessage);
				// 송신한 메시지을 표시.
				this.ta_result.append(this.lobbyform.getUsername() + ": "
						+ sendmessage + "\n");
				this.tf_message.setText("");
				// 스크롤 위치 조정.
				this.ta_result.setCaretPosition(ta_result.getText().length());
			}
		}
	}

}
