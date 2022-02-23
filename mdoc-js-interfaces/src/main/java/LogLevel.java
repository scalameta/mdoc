package mdoc.js.interfaces;

public enum LogLevel {
  Debug(1), Info(2), Warning(3), Error(4);

  private final int order;
  private LogLevel(int order) {
      this.order = order;
  }
  public int getOrder() {
      return order;
  }
}
