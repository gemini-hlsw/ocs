package edu.gemini.util;

import java.util.concurrent.locks.ReentrantLock;

class WaitableEvetSetTester {

	enum Event { FOO, BAR, BAZ };

	public static void main(String[] args) throws InterruptedException {

		final ReentrantLock lock = new ReentrantLock();
		final WaitableEventSet<Event> waitable = new WaitableEventSet<Event>(lock);

		new Thread() {
			@Override
			public void run() {
				for (;;) {
					try {
						lock.lock();
						Event e = waitable.await(500, Event.FOO);
						System.out.println(this + ": " + e);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					} finally {
						lock.unlock();
					}
				}
			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				for (;;) {
					for (;;) {
						try {
							lock.lock();
							Event e = waitable.await(Event.FOO, Event.BAR);
							System.out.println(this + ": " + e);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						} finally {
							lock.unlock();
						}
					}
				}
			}
		}.start();

		for (Event e: Event.values()) {
			Thread.sleep(1000);
			lock.lock();
			waitable.signal(e);
			lock.unlock();
		}

	}

}
