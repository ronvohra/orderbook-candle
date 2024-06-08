package io.gsr.orderbookcandle.domain;

public class MidPrices {
  private double openMidPrice;
  private double closeMidPrice;
  private double highMidPrice;
  private double lowMidPrice;

  public double getOpenMidPrice() {
    return openMidPrice;
  }

  public void setOpenMidPrice(double openMidPrice) {
    this.openMidPrice = openMidPrice;
  }

  public double getCloseMidPrice() {
    return closeMidPrice;
  }

  public void setCloseMidPrice(double closeMidPrice) {
    this.closeMidPrice = closeMidPrice;
  }

  public double getHighMidPrice() {
    return highMidPrice;
  }

  public void setHighMidPrice(double highMidPrice) {
    this.highMidPrice = highMidPrice;
  }

  public double getLowMidPrice() {
    return lowMidPrice;
  }

  public void setLowMidPrice(double lowMidPrice) {
    this.lowMidPrice = lowMidPrice;
  }
}
