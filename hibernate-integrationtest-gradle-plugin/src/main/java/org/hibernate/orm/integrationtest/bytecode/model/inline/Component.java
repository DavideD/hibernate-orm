/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
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
