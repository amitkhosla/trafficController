package com.samples.stocksSample.normal.dto;

public class Transaction {
	private Long userId;
	private String stock;
	private BuySell buyOrSell;
	private Integer qty;
	public Long getUserId() {
		return userId;
	}
	public Transaction setUserId(Long userId) {
		this.userId = userId;
		return this;
	}
	public String getStock() {
		return stock;
	}
	public Transaction setStock(String stock) {
		this.stock = stock;
		return this;
	}
	public BuySell getBuyOrSell() {
		return buyOrSell;
	}
	public Transaction setBuyOrSell(BuySell buyOrSell) {
		this.buyOrSell = buyOrSell;
		return this;
	}
	public Integer getQty() {
		return qty;
	}
	public Transaction setQty(Integer qty) {
		this.qty = qty;
		return this;
	}
}
