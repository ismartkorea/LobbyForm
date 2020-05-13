package net.hibiznet.libworks;

/**
 * 
 * @author hibiznet
 *
 */
public interface GameClientListener {
	//커맨드수신시에 불러지는 메소드.
	//commnad가 0인 경우 서버에서 에러머시지.
	public void receive(int command, int startpt, int endpt, String body);
	//수신에러가 발생한 경우에 불러지는 메소드.
	//필요없는 경우 아무것도 안함.
	public void receiveError();
	//수신용타이머을 다른 곳에 사용할 때.
	public void receiveTimerRun();
}
