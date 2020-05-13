package net.hibiznet.libworks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * 
 * @author hibiznet
 *
 */
public class LobbyForm extends JDialog implements GameClientListener,
		ActionListener, ListSelectionListener {

	private static final long serialVersionUID = 1L;

	private int userID = -1; // User ID
	private String username; // User 명
	private int numrooms = 0; // ROOM 수.
	private int roomID = -1; // ROOM ID
	// 유저ID와 유저명의 대응표.
	private HashMap<String, Integer> usermap = new HashMap<String, Integer>();
	private boolean multiplay = false;
	private int oppID = -1; // 대전상대ID
	private String oppname; // 대전상대의 이름.
	private boolean oppstatus = false; // 대전상대의 상태.

	//
	private boolean succeed = false;

	// 긓깛긣깓�[깑쀞
	private JButton btn_joinRoom = new JButton("입장");
	private JButton btn_offer = new JButton("대전 신청중.");
	private GameCommunicator communicator = null;
	private DefaultListModel model_users = new DefaultListModel();
	private JList lst_users = new JList(this.model_users);
	private JComboBox cmb_rooms = new JComboBox();
	private JTextArea ta_userinfo = new JTextArea(0, 10);

	// Constrant
	// 부모프레임, 서버와츼 통신채널, 게임로그인용 ID와 패스워드
	public LobbyForm(JFrame mainframe, GameCommunicator gcom,
			String gameIDPD) {
		super(mainframe, "게임 로비", false);
		this.communicator = gcom;

		Container cpane = this.getContentPane();
		JScrollPane jspane1 = new JScrollPane(this.lst_users);
		this.lst_users.setForeground(Color.WHITE);
		this.lst_users.setBackground(Color.BLACK);
		jspane1.setPreferredSize(new Dimension(360, 160));
		cpane.add(jspane1, BorderLayout.CENTER);
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.add(this.cmb_rooms);
		panel2.add(this.btn_joinRoom);
		panel2.add(this.btn_offer);
		cpane.add(panel2, BorderLayout.SOUTH);
		this.ta_userinfo.setEditable(false);
		this.ta_userinfo.setBackground(Color.LIGHT_GRAY);
		cpane.add(this.ta_userinfo, BorderLayout.EAST);
		// cpane.add(this.lbl_userstatus, BorderLayout.EAST); //TODO

		this.pack();
		this.setLocationRelativeTo(null);

		// 액선 설정.
		this.btn_joinRoom.addActionListener(this);
		this.btn_offer.addActionListener(this);
		this.cmb_rooms.addActionListener(this);
		this.lst_users.addListSelectionListener(this);

		// 다이어로그 박스가 비표시인 경우 리스너 삭제.
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentHidden(ComponentEvent arg0) {
				communicator.removeGameTimerListener();
				communicator.removeGameClientListener();
				btn_offer.setEnabled(true);
			}
		});
	}

	// Get, Set
	public void setUsername(String username) {
		this.username = username;
		this.setTitle("긒�[�깓긮�[갌" + this.username);
	}

	public String getUsername() {
		return username;
	}

	public int getRoomID() {
		return roomID;
	}

	public int getUserID() {
		return this.userID;
	}

	// 멀티대전모드은 상대가 대전중인 상태에서도 로그인 가능.
	// default은 false
	public void setMultiplay(boolean multiplay) {
		this.multiplay = multiplay;
	}

	public HashMap<String, Integer> getUserMap() {
		return this.usermap;
	}

	public int getOpponentID() {
		return this.oppID;
	}

	public String getOpponentName() {
		return this.oppname;
	}
	//대전 준비가 된 경우은 true
	public boolean getSucceed(){
		return this.succeed;
	}

	// 로비폼 표시.
	public void showRobby() {
		this.oppID = -1;
		this.oppname = "";
		this.oppstatus = false;
		this.succeed = false;

		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		
		this.communicator.setGameClientListener(this);
		this.communicator.setGameTimerListener(this);
		// 자기 유저 ID를 확인.
		this.communicator.sendToServer(GameCommunicator.COM_GETUSERID, 0, 0,
				null);
		// 서버에 ROOM수 확인.
		this.communicator
				.sendToServer(GameCommunicator.COM_ROOMNUM, 0, 0, null);
		// 자신을 비대전중으로 전환.
		this.communicator.sendToServer(GameCommunicator.COM_SETSELFSTAT,
				this.userID, 0, Code64.encodeBoolean(false).toString());
	}

	// 데이타수신시에 불러지는 메소드.
	@Override
	public void receive(int command, int startpt, int endpt, String body) {
		switch (command) {
		case -GameCommunicator.COM_GETUSERID:
			// 유저ID 취득.
			if (body != null) {
				this.userID = Code64.decodeShort(body);
			}
			break;
		case -GameCommunicator.COM_ROOMNUM:
			// ROOM수 문의.
			if (body != null) {
				// ROOM 수 취득.
				this.numrooms = Code64.decodeShort(body);
				// ROOM에 대응하는 LIST을 추가.
				for (int i = 0; i < this.numrooms; i++) {
					this.cmb_rooms.addItem("Room" + i);
				}
				// ROOM 멤버 확인 개시.
				this.communicator.sendToServer(GameCommunicator.COM_MEMBERNUM,
						0, 0, "000");
			}
			break;
		case -GameCommunicator.COM_JOINROOM:
			// 입장 문의 
			if (body != null) {
				// 입장한 ROOM ID 취득.
				this.roomID = Code64.decodeShort(body);
				// ROOM 멤버 확인.
				this.communicator.sendToServer(GameCommunicator.COM_MEMBERNUM,
						0, 0, body);
				// 다음 갱신이 잠시 하지 않는 경우.
				this.reloadcounter = 1;
			}
			break;
		case -GameCommunicator.COM_MEMBERNUM:
			// 지정한 ROOM 멤버 수 문의 반환.
			// 계속 ROOM내의 멤버 소개 개시됨.
			if (body != null) {
				int membernum = Code64.decodeShort(body);
				this.model_users.clear();
				this.usermap.clear();
				if (membernum > 0) {
					for (int i = 0; i < membernum; i++) {
						this.model_users.addElement("member" + i);
					}
					// 선택ROOM 0번째 유저ID와 이름 취득.
					this.communicator.sendToServer(
							GameCommunicator.COM_REFMEMBER, this.userID, 0,
							Code64.encodeShort(
									(short) this.cmb_rooms.getSelectedIndex())
									.append(Code64.encodeShort((short) 0))
									.toString());
				}
			}
			break;
		case -GameCommunicator.COM_REFMEMBER:
			// 멤버 조회 반환.
			if (body != null) {
				// body 데이타 취득.
				int refroomID = Code64.decodeShort(body.substring(0, 3));
				int refuseridx = Code64.decodeShort(body.substring(3, 6));
				int refuserID = Code64.decodeShort(body.substring(6, 9));
				String refusername = body.substring(9);
				// 선택한 ROOM이 갱신되어 있는 지 확인.
				if (refroomID == this.cmb_rooms.getSelectedIndex()) {
					// 만일에 대비해 리스트의 등록수가 유저 인덱스보다 윗인 지 확인.
					if (refuseridx < this.model_users.getSize()) {
						// 리스트 유저 명 갱신.
						this.model_users.set(refuseridx, refusername);
						// 유저명과 유저ID 관계 등록.
						this.usermap.put(refusername, refuserID);
						// 최종 유저가 없으면 다음 조회 실행.
						if (refuseridx < this.model_users.getSize() - 1) {
							this.communicator
									.sendToServer(
											GameCommunicator.COM_REFMEMBER,
											this.userID,
											0,
											Code64
													.encodeShort(
															(short) this.cmb_rooms
																	.getSelectedIndex())
													.append(
															Code64
																	.encodeShort((short) (refuseridx + 1)))
													.toString());
						}
					}
				}
			}
			break;
		case -GameCommunicator.COM_REFUSERSTAT:
			// 멤버 정보 소개.
			if (body != null) {
				this.oppstatus = Code64.decodeBoolean(body.substring(3));
				if (this.oppstatus == true) {
					this.ta_userinfo.append("status: 긵깒귽뭷");
					this.ta_userinfo.setBackground(Color.LIGHT_GRAY);
				} else {
					this.ta_userinfo.append("status: 뫲�@");
					this.ta_userinfo.setBackground(new Color(200,255,200));
				}
			}
			break;
		case GameCommunicator.COM_OFFERPLAY:
			// 대전 신청한 경우.
			int result = JOptionPane.showConfirmDialog(this, body
					+ "가 대전을 신청했습니다.", "게임 로비", JOptionPane.YES_NO_OPTION);
			boolean accept = true;
			if (result == JOptionPane.NO_OPTION)
				accept = false;
			// 수신을 보냄.(true이면 대전수락, false이면 거절)
			this.communicator.sendToServer(-GameCommunicator.COM_OFFERPLAY,
					endpt, startpt, Code64.encodeBoolean(accept).toString());
			// 신청을 받은 경우 로그인 폼을 닫음.
			if (accept) {
				this.oppname = body;
				this.oppID = startpt;
				this.setSucceed();
			}
			break;
		case -GameCommunicator.COM_OFFERPLAY:
			// 대전신청 답변이 오면
			if (body != null) {
				boolean reply = Code64.decodeBoolean(body);
				if (reply == true) {
					// 대전을 받아 드림.
					this.oppID = startpt;
					this.setSucceed();
				} else {
					// 대전을 거절당한
					// 대전 버튼을 유효로 바뀜.
					this.btn_offer.setEnabled(true);
					JOptionPane.showMessageDialog(this, "대전신청을 받아들이지 않았습니다.");
				}
			}
			break;
		}
	}

	//대전준비완료.
	private void setSucceed(){
		this.succeed = true;
		// 자신을 대전중으로 한다.
		this.communicator.sendToServer(
				GameCommunicator.COM_SETSELFSTAT, this.userID, 0,
				Code64.encodeBoolean(true).toString());
		// 로그인 폼을 닫는다.
		this.setVisible(false);
	}

	@Override
	public void receiveError() {
	}

	// 버튼이 조작되면 불러지는 메소드.
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.btn_joinRoom) {
			// 입장 버튼을 눌러지면
			// 콤보박스 선택 인덱스을 취득.
			int roomID = this.cmb_rooms.getSelectedIndex();
			this.communicator.sendToServer(GameCommunicator.COM_JOINROOM,
					this.userID, 0, Code64.encodeShort((short) roomID)
							.toString());
		} else if (e.getSource() == this.cmb_rooms) {
			// ROOM 멤버 확인을 개시.
			this.communicator.sendToServer(GameCommunicator.COM_MEMBERNUM, 0,
					0, Code64.encodeShort(
							(short) this.cmb_rooms.getSelectedIndex())
							.toString());
			// 다음 갱신이 잠깐 하지 않을 경우.
			this.reloadcounter = 1;
		} else if (e.getSource() == this.btn_offer) {
			// 대전버튼이 눌러진.
			// 유저가 1인이상인지 확인.
			if (this.model_users.size() < 1) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
			// 아직 입장하지 않는 (roomID가-1) 또는 다른 ROOM 이면 에러.
			if (this.roomID != this.cmb_rooms.getSelectedIndex()) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(this, "먼저 입장해주세요.");
				return;
			}
			// 대전신청을 하고 있는 상대가 자신이면 에러.
			if (this.userID == this.oppID) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
			if (this.multiplay == true) {
				// 멀티플레이 모드인 경우 대전관계없이 대전개시.
				this.setSucceed();
			} else {
				// 대전 모드
				if (this.oppstatus == true) {
					// 상대가 대전중인 경우 에러.
					java.awt.Toolkit.getDefaultToolkit().beep();
				} else {
					// 대전신청을 반환한다.
					this.communicator.sendToServer(
							GameCommunicator.COM_OFFERPLAY, this.userID,
							this.oppID, this.username);
					// 대전버튼을 무효로 한다.
					this.btn_offer.setEnabled(false);
				}
			}
		}
	}

	private int reloadcounter = 0;

	// 데이타수신정보에 대해서 불러지는 타이머.
	@Override
	public void receiveTimerRun() {
		// 타이머을 사용하고 600회에 1ㅣ회(30초에 1회) 현재 ROOM 멤버 정보을 갱신.
		reloadcounter++;
		reloadcounter = reloadcounter % 600;
		if (reloadcounter == 0) {
			// 깑�[�긽깛긫�[궻둴봃귩둎럑
			this.communicator.sendToServer(GameCommunicator.COM_MEMBERNUM, 0,
					0, Code64.encodeShort(
							(short) this.cmb_rooms.getSelectedIndex())
							.toString());
		}
	}

	// 유저 리스트 선택시에 불러지는 메서드.
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == this.lst_users) {
			if (e.getValueIsAdjusting()) {

				// 유저 1인이상인지 확인.
				if (this.model_users.size() > 0) {
					String refusername = (String) this.lst_users
							.getSelectedValue();
					// 이름과 대전 USER ID 취득.
					Integer refuserID = this.usermap.get(refusername);
					if (refuserID != null) {
						// 정보파넬 테그슽 갱신.
						this.ta_userinfo.setText("name: " + refusername + "\n");
						if (this.userID == refuserID) {
							this.ta_userinfo.append("(YOU)\n");
						}
						this.ta_userinfo.append("ID: " + refuserID + "\n");
						// ROOM 멤버 상태확인 개시.
						this.communicator.sendToServer(
								GameCommunicator.COM_REFUSERSTAT, 0, 0, Code64
										.encodeShort(refuserID.shortValue())
										.toString());
						this.oppID = refuserID;
						this.oppname = refusername;
					}
				}
			}
		}

	}

}
