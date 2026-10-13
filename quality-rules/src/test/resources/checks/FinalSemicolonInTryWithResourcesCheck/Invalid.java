class Sample { void run() throws Exception { try (AutoCloseable close = open();) {} } }
