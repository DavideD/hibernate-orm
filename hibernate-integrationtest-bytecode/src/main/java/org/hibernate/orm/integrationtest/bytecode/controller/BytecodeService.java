/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.integrationtest.bytecode.controller;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.orm.integrationtest.bytecode.model.Book;
import org.hibernate.orm.integrationtest.bytecode.model.Person;

@Stateful
public class BytecodeService {

	@PersistenceContext
	public EntityManager em;

	public void persist(Object... entities) {
		for ( Object entity : entities ) {
			em.persist( entity );
		}
	}

	public Person findAuthor( Long authorId ) {
		return em.find( Person.class, authorId );
	}

	public Person findAuthorForBook(Book book) {
		return em.find( Book.class, book.getId() ).getAuthor();
	}

	public Book findBook(Long bookdId) {
		return em.find( Book.class, bookdId );
	}
}

