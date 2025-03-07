/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.data.jdbc.h2;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Where;
import io.micronaut.data.exceptions.EmptyResultException;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.tck.entities.Author;
import io.micronaut.data.tck.entities.Book;

import javax.transaction.Transactional;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JdbcRepository(dialect = Dialect.H2)
public abstract class H2BookRepository extends io.micronaut.data.tck.repositories.BookRepository {
    private final JdbcOperations jdbcOperations;

    public H2BookRepository(JdbcOperations jdbcOperations, H2AuthorRepository authorRepository) {
        super(authorRepository);
        this.jdbcOperations = jdbcOperations;
    }

    @Query("UPDATE book SET total_pages = :pages WHERE title = :title")
    abstract Long setPages(int pages, String title);

    @Query("DELETE book WHERE title = :title")
    abstract Long wipeOutBook(String title);

    @Transactional
    public Author findByName(String name) {
        return jdbcOperations.prepareStatement("SELECT author_.id,author_.name,author_.nick_name,author_books_.id AS _books_id,author_books_.author_id AS _books_author_id,author_books_.title AS _books_title,author_books_.total_pages AS _books_total_pages,author_books_.publisher_id AS _books_publisher_id,author_books_.last_updated AS _books_last_updated FROM author AS author_ INNER JOIN book author_books_ ON author_.id=author_books_.author_id WHERE (author_.name = ?)", statement -> {
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Author author = jdbcOperations.readEntity(resultSet, Author.class);
                Set<Book> books = new HashSet<>();
                do {
                    books.add(jdbcOperations.readEntity("_books_", resultSet, Book.class));
                } while (resultSet.next());
                author.setBooks(books);
                return author;
            }
            throw new EmptyResultException();
        });
    }

    @Where(value = "total_pages > :pages")
    abstract List<Book> findByTitleStartsWith(String title, int pages);

    @Query(value = "select count(*) from book b where b.title like :title and b.total_pages > :pages", nativeQuery = true)
    abstract int countNativeByTitleWithPagesGreaterThan(String title, int pages);
}
