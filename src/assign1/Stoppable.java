package assign1;

public class Stoppable extends Thread {
	protected boolean shutdown;
	protected boolean timeout;
	
	
	public void setShutdown() {
		shutdown = true;
	}
	
	
}
