/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.integrationtest.bytecode.model.bidirectional;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;

@Entity
public class Book {

	@Id
	private Long id;

	private String title;

	@NaturalId
	private String isbn;

	@ManyToOne
	private Person author;

	public Book() {
	}

	public Book(Long id, String title, String isbn) {
		this.id = id;
		this.title = title;
		this.isbn = isbn;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public Person getAuthor() {
		return author;
	}

	public void setAuthor(Person author) {
		this.author = author;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Book book = (Book) o;
		return Objects.equals( title, book.title ) &&
				Objects.equals( isbn, book.isbn );
	}

	@Override
	public int hashCode() {
		return Objects.hash( title, isbn );
	}

	@Override
	public String toString() {
		return title + ", '" + isbn;
	}
}

