/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.sql.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.sql.ast.spi.JdbcParameterRenderer;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @implNote Restricted to H2 as there is nothing intrinsically Dialect specific here,
 * though each database has specific syntax for labelled parameters
 */
@ServiceRegistry( services = @ServiceRegistry.Service(
		role = JdbcParameterRenderer.class,
		impl = FilterWithParameterRendererTest.DollarParameterRendererImpl.class
) )
@DomainModel( annotatedClasses = { FilterWithParameterRendererTest.Element.class, FilterWithParameterRendererTest.Node.class} )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialect( H2Dialect.class )
public class FilterWithParameterRendererTest {

	@Test
	public void testParameterRendering(SessionFactoryScope scope) {
		final String queryString = "select distinct n from Node n left join fetch n.elements order by n.id";
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		final String expectedSql = "select distinct n1_0.id,e1_0.node_id,e1_0.id,e1_0.deleted,e1_0.region,n1_0.parent_id,n1_0.region,n1_0.string,n1_0.version from Node n1_0 left join Element e1_0 on n1_0.id=e1_0.node_id and e1_0.region = $1 where n1_0.region = $2 order by n1_0.id";
		scope.inTransaction( session -> {
			session.enableFilter( "region" ).setParameter( "region", "oceania" );
			session
					.createSelectionQuery( queryString, Node.class )
					.list();
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		final String sql = statementInspector.getSqlQueries().get( 0 );
		assertThat( sql ).contains( "$1" );
		assertThat( sql ).contains( "$2" );
		assertThat( sql ).isEqualTo( expectedSql );
	}

	public static class DollarParameterRendererImpl implements JdbcParameterRenderer {
		@Override
		public void renderJdbcParameter(int position, JdbcType jdbcType, SqlAppender appender, Dialect dialect) {
			jdbcType.appendWriteExpression( "$" + position, appender, dialect );
		}
	}

	@FilterDef(name = "current", defaultCondition = "deleted = false")
	@FilterDef(name = "region", defaultCondition = "region = :region", parameters = @ParamDef(name = "region", type = org.hibernate.type.descriptor.java.StringJavaType.class))
	@Entity(name = "Element")
	@Table(name = "Element")
	@Filter(name = "current")
	@Filter(name = "region")
	public static class Element {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Node node;

		private boolean deleted;

		private String region;

		public Element(Node node) {
			this.node = node;
		}

		Element() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Node getNode() {
			return node;
		}

		public void setNode(Node node) {
			this.node = node;
		}

		public boolean isDeleted() {
			return deleted;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}
	}

	@Entity(name = "Node")
	@Table(name = "Node")
	@Filter(name = "current")
	@Filter(name = "region")
	public static class Node {

		@Id
		@GeneratedValue
		private Integer id;
		@Version
		private Integer version;
		private String string;

		private String region;

		@ManyToOne(fetch = FetchType.LAZY,
				cascade = {
						CascadeType.PERSIST,
						CascadeType.REFRESH,
						CascadeType.MERGE,
						CascadeType.REMOVE
				})
		private Node parent;

		@OneToMany(fetch = FetchType.LAZY,
				cascade = {
						CascadeType.PERSIST,
						CascadeType.REMOVE
				},
				mappedBy = "node")
		@Filter(name = "current")
		@Filter(name = "region")
		private List<Element> elements = new ArrayList<>();

		public Node(String string) {
			this.string = string;
		}

		Node() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

		public Node getParent() {
			return parent;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

		public List<Element> getElements() {
			return elements;
		}

		public void setElements(List<Element> elements) {
			this.elements = elements;
		}

		@Override
		public String toString() {
			return id + ": " + string;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Node node = (Node) o;
			return Objects.equals( string, node.string );
		}

		@Override
		public int hashCode() {
			return Objects.hash( string );
		}
	}
}
