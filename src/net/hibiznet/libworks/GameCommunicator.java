package net.hibiznet.libworks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;

/**
 * 
 * @author hibiznet
 *
 */
public class GameCommunicator {

	// 수신용 필드.
	private static final int BUFMAXSIZE = 1024;
	private SocketChannel channel = null;
	private Charset charset = Charset.forName("UTF-8");
	private ByteBuffer readbuf = ByteBuffer.allocate(BUFMAXSIZE);
	private CharBuffer sendbuf = CharBuffer.allocate(BUFMAXSIZE);

	private GameClientListener gclistener = null;
	private GameClientListener gtlistener = null;
	private Timer timer;
	private boolean gamelogin = false;

	// 서버 접속.
	public void connectServer(String hostname, int waitport, String gameIDPD) {
		// 서버 접속, 채널 오픈
		try {
			this.channel = SocketChannel.open(new InetSocketAddress(hostname,
					waitport));
			this.channel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// 수신감시용 타이머.
		// 불러낸 회수 1초간에 20회
		this.timer = new Timer();
		this.timer.schedule(new ReceiveTimerTask(), 0, 50);

		// 서버에 로그인 커맨드 보냄.
		this.sendToServer(1, 0, 0, gameIDPD);
	}

	// 로그인 완료 확인.
	// 비동기통신이라서 비접속확인이 가능할때까지 일정시간 대기.
	public boolean isLogin() {
		// 1초당 10회확인.
		for (int i = 0; i < 10; i++) {
			if (this.gamelogin == true)
				return true;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	// 서버 접속 끊김
	public void disconnect() {
		try {
			this.timer.cancel();
			if (this.channel != null) {
				this.channel.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 서버에 메시지을 송신함.
	// body가 없는 경우는 null을 지정.
	public void sendToServer(int command, int startpt, int endpt, String body) {
		// 커맨드 엔코드
		this.sendbuf.clear();
		this.sendbuf.append((char) 02);
		this.sendbuf.append('1');
		this.sendbuf.append(Code64.encodeShort((short) command));
		this.sendbuf.append(Code64.encodeShort((short) startpt));
		this.sendbuf.append(Code64.encodeShort((short) endpt));
		if (body != null) {
			this.sendbuf.append(Code64.encodeShort((short) body.length()));
			this.sendbuf.append(body);
		} else {
			this.sendbuf.append("000");
		}
		this.sendbuf.append((char) 03);
		this.sendbuf.flip();

		System.out.println("송신: " + this.sendbuf.toString());
		if (channel.isConnected()) {
			try {
				this.channel.write(charset.encode(this.sendbuf));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			System.out.println("서버로부터 끊겼습니다.");
		}
	}

	// 이 컴퍼넌트와 리스너을 관련붙여
	// 주:모두 추가되어 있는 지 어떤지을 체크은 하지 않음.
	// 결국, 새로운 리스너가 등록되어지면 이전 리스너는 불러지지 않는다.
	public void setGameClientListener(GameClientListener listener) {
		this.gclistener = listener;
	}

	public void removeGameClientListener() {
		this.gclistener = null;
	}

	// 이 콤퍼넌트가 대기 하는 타이머을 수신이외의 것으로 사용한다.
	public void setGameTimerListener(GameClientListener listener) {
		this.gtlistener = listener;
	}

	public void removeGameTimerListener() {
		this.gtlistener = null;
	}

	// 수신처리용 타이머 테스크.
	class ReceiveTimerTask extends TimerTask {

		@Override
		public void run() {
			if (channel.isOpen()) {
				// 데이타 수신처리.
				try {
					readbuf.clear();
					if (channel.read(readbuf) > 0) {
						readbuf.flip();
						CharBuffer cbuf = charset.decode(readbuf);

						// 버퍼 데이타로부터 커맨드을 잘라내고 해독.
						// 에러 대처 mark을 limit위치에 설정해 둠.
						cbuf.position(cbuf.limit());
						cbuf.mark();
						cbuf.flip();
						while (cbuf.position() < cbuf.limit()) {
							// 개시 코드 02을 검색.
							if (cbuf.charAt(0) == (char) 02) {
								cbuf.mark(); // 개시 위치 02을 마크.
							}
							// 종료 코드 03을 검색.
							if (cbuf.charAt(0) == (char) 03) {
								int pos = cbuf.position(); // 03 있는 위치를 기록.
								cbuf.reset(); // 현재위치을 마크에 돌림.
								// 03이 02보다 뒤에 있는 것을 확인.
								if (pos > cbuf.position()) {
									// 현재위치 이후을 디코드.
									this.decode(cbuf.slice());
								} else {
									// 수신에러 : 03 전에 02가 찾지 않음.
									this.recieveError();
								}
								// 현재위치을 03 있는 위치에 이동.
								cbuf.position(pos);
							}
							cbuf.get();
						}
						// 수신에러 체크:
						// 정상적인 커맨드가 처리되어 있으면 mark은 최후커맨드가 선두로 있을 거임.
						// 결국 mark가 limit위치에 있으면 아무것도 발견되지 않음.
						cbuf.reset(); // 현위치을 마크로 돌림.
						if (cbuf.limit() == cbuf.position()) {
							this.recieveError();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"서버에서 네트워크 경로에 장해가 밸생했습니다만,\n" + "서버가 다운된 가능성이 있습니다.\n"
									+ "프로그램을 강제종료합니다.", "치명적인 에러.",
							JOptionPane.WARNING_MESSAGE);
					System.exit(-1);
				}
			}
			if (gtlistener != null) {
				gtlistener.receiveTimerRun();
			}
		}

		private void decode(CharBuffer cbuf) {
			System.out.println("수신: " + cbuf.toString());
			// 로그인 성공 확인.
			int command = Code64.decodeShort(cbuf.subSequence(2, 5).toString());
			if (command == -1) {
				gamelogin = true;
			}

			// 이벤트 핸들러 호출.
			if (gclistener != null) {
				int startpt = Code64.decodeShort(cbuf.subSequence(5, 8)
						.toString());
				int endpt = Code64.decodeShort(cbuf.subSequence(8, 11)
						.toString());
				int bodylen = Code64.decodeShort(cbuf.subSequence(11, 14)
						.toString());
				if (bodylen > 0) {
					gclistener.receive(command, startpt, endpt, cbuf
							.subSequence(14, 14 + bodylen).toString());
				} else {
					gclistener.receive(command, startpt, endpt, null);
				}
			}
		}

		// 수신에러 발생.
		private void recieveError() {
			System.out.println("수신했으나 해독 불가능");
			if (gclistener != null) {
				gclistener.receiveError();
			}
		}
	}

	public static final int COM_GAMELOGIN = 1;
	public static final int COM_GETUSERID = 2;
	public static final int COM_SETUSERNAME = 3;
	public static final int COM_ROOMNUM = 4;
	public static final int COM_MEMBERNUM = 5;
	public static final int COM_OPENSEAT = 6;
	public static final int COM_REFMEMBER = 7;
	public static final int COM_REFUSERSTAT = 8;
	public static final int COM_JOINROOM = 9;
	public static final int COM_SETSELFSTAT = 10;
	public static final int COM_LEAVEROOM = 11;
	public static final int COM_LEAVEGAME = 12;
	public static final int COM_OFFERPLAY = 13;

}
