package com.example.drink_order_system;

public class Flavor {
	private String size;
	private String temperature;
	private String sugar;

	public Flavor(String size, String temperature, String sugar) {
		this.size = size;
		this.temperature = temperature;
		this.sugar = sugar;
	}

	// region Getter Methods
	public String getSize() { return size; }
	public String getTemperature() { return temperature; }
	public String getSugar() { return sugar; }
	// endregion

	@Override
	public String toString() {
		return String.format("规格: %s | 温度: %s | 甜度: %s", size, temperature, sugar);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Flavor flavor = (Flavor) obj;
		return size.equals(flavor.size) &&
				temperature.equals(flavor.temperature) &&
				sugar.equals(flavor.sugar);
	}
}