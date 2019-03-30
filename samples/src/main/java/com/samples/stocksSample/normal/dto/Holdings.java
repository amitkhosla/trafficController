package com.samples.stocksSample.normal.dto;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Holdings {
	private Long userId;
	private Map<String, StockHolding> holdings = new ConcurrentHashMap<>();
	public Long getUserId() {
		return userId;
	}
	public Holdings setUserId(Long userId) {
		this.userId = userId;
		return this;
	}
	
	public Holdings addHolding(StockHolding holding) {
		holdings.put(holding.getStock(), holding);
		return this;
	}
	
	public Holdings addQuantity(String stock, Integer qty) {
		StockHolding holding = holdings.get(stock);
		if (Objects.isNull(holding)) {
			holding = new StockHolding().setQuantity(qty).setStock(stock);
			addHolding(holding);
		} else {
			holding.addQty(qty);
		}
		return this;
	}
	
	public Holdings reduceQuantity(String stock, Integer qty) {
		return addQuantity(stock, qty * -1);
	}
}
