package org.hibernate.orm.integrationtest.bytecode.model.inline;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Component {
	@Column(name = "first_player_token")
	private String component;

	public Component() {
	}

	public Component(String component) {
		this.component = component;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}
}