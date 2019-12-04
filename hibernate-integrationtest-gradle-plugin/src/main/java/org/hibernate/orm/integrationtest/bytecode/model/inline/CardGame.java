/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.integrationtest.bytecode.model.inline;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "CardGame")
public class CardGame {

	@Id
	private String id;
	private String name;

	@Embedded
	private Component firstPlayerToken;

	public CardGame() {
	}

	private CardGame(String id, String name) {
		this.id = id;
		this.name = name;
		this.firstPlayerToken = createEmbeddedValue( name );
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.firstPlayerToken = createEmbeddedValue( name );
	}

	public Component getFirstPlayerToken() {
		return firstPlayerToken;
	}

	public void setFirstPlayerToken(Component firstPlayerToken) {
		this.firstPlayerToken = firstPlayerToken;
	}

	private Component createEmbeddedValue(String name) {
		return new Component( name + " first player token" );
	}
}

