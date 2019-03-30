package com.samples.stocksSample.normal.dto;

public class StockHolding {
	private String stock;
	private Integer quantity;
	
	public String getStock() {
		return stock;
	}
	public StockHolding setStock(String stock) {
		this.stock = stock;
		return this;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public StockHolding setQuantity(Integer quantity) {
		this.quantity = quantity;
		return this;
	}
	public StockHolding addQty(int qty) {
		this.quantity = this.quantity + qty;
		return this;
	}
	public StockHolding reduceQty(int qty) {
		this.quantity = this.quantity - qty;
		return this;
	}
	
}
