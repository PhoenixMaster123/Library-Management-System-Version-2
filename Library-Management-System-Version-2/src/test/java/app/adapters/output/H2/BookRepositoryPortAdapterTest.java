package app.adapters.output.H2;

/*
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class BookRepositoryPortAdapterTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;
    @InjectMocks
    BookRepositoryPortAdapter dao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dao = new BookRepositoryPortAdapter(bookRepository, authorRepository);
    }
    @Test
    void test_saveBook_Success() {
        Book book = new Book(UUID.randomUUID(), "Book Test", "9876543210", 2022, true, LocalDate.now());
        book.setAuthors(new HashSet<>(Arrays.asList(
                new Author("New Author 1", "Bio 1"),
                new Author("New Author 2", "Bio 2")
        )));

        AuthorEntity author1 = new AuthorEntity(UUID.randomUUID(), "New Author 1", "Bio 1", new HashSet<>());
        AuthorEntity author2 = new AuthorEntity(UUID.randomUUID(), "New Author 2", "Bio 2", new HashSet<>());

        when(authorRepository.findByName("New Author 1")).thenReturn(Optional.empty());
        when(authorRepository.findByName("New Author 2")).thenReturn(Optional.empty());
        when(authorRepository.save(any(AuthorEntity.class))).thenReturn(author1, author2);
        when(bookRepository.save(any(BookEntity.class))).thenReturn(new BookEntity());

        dao.saveBook(book);

        verify(authorRepository, times(2)).save(any(AuthorEntity.class));
        verify(bookRepository).save(any(BookEntity.class));
    }
    @Test
    public void testUpdateBook_updatesExistingBook() {
        UUID bookId = UUID.randomUUID();
        Book updatedBook = new Book("Updated Title", "9876543210", 2024, false, LocalDate.now());
        BookEntity existingBookEntity = new BookEntity();
        existingBookEntity.setBookId(bookId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBookEntity));

        BookRepositoryPortAdapter dao = new BookRepositoryPortAdapter(bookRepository, authorRepository);
        dao.updateBook(bookId, updatedBook);

        verify(bookRepository).findById(bookId);
        verify(bookRepository).save(existingBookEntity);
        assertEquals(existingBookEntity.getTitle(), updatedBook.getTitle());
        assertEquals(existingBookEntity.getIsbn(), updatedBook.getIsbn());
        assertEquals(existingBookEntity.getPublicationYear(), updatedBook.getPublicationYear());
        assertEquals(existingBookEntity.isAvailability(), updatedBook.isAvailable());
    }

    @Test
    public void testUpdateBook_doesNothingIfBookNotFound() {
        UUID bookId = UUID.randomUUID();
        Book updatedBook = new Book("Updated Title", "9876543210", 2024, false, LocalDate.now());
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        BookRepositoryPortAdapter dao = new BookRepositoryPortAdapter(bookRepository, authorRepository);
        dao.updateBook(bookId, updatedBook);

        verify(bookRepository).findById(bookId);
        verify(bookRepository, never()).save(any(BookEntity.class));
    }

    @Test
    public void testDeleteBook_deletesBookAndRemovesAssociations() {
        UUID bookId = UUID.randomUUID();
        BookEntity existingBookEntity = new BookEntity();
        existingBookEntity.setBookId(bookId);
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.getBooks().add(existingBookEntity);
        existingBookEntity.setAuthors(Set.of(authorEntity));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBookEntity));

        BookRepositoryPortAdapter dao = new BookRepositoryPortAdapter(bookRepository, authorRepository);
        dao.deleteBook(bookId);

        verify(bookRepository).findById(bookId);
        verify(bookRepository).deleteById(bookId);
        assertTrue(authorEntity.getBooks().isEmpty());
    }

    @Test
    public void testDeleteBook_throwsExceptionIfBookNotFound() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        BookRepositoryPortAdapter dao = new BookRepositoryPortAdapter(bookRepository, authorRepository);
        assertThrows(BookNotFoundException.class, () -> dao.deleteBook(bookId));
    }

    @Test
    void test_getPaginatedBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        BookEntity bookEntity = new BookEntity(UUID.randomUUID(), "Book 1", "1234567890", 2022, true, LocalDate.now(), new HashSet<>(), new ArrayList<>());
        Page<BookEntity> page = new PageImpl<>(Collections.singletonList(bookEntity));

        when(bookRepository.findAll(pageable)).thenReturn(page);

        Page<Book> result = dao.getPaginatedBooks(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Book 1", result.getContent().getFirst().getTitle());
    }
    @Test
    void test_searchBookByTitle() {
        String title = "Book Test";
        BookEntity bookEntity = new BookEntity(UUID.randomUUID(), title, "9876543210", 2022, true, LocalDate.now(), new HashSet<>(), new ArrayList<>());
        when(bookRepository.findBookByTitle(title)).thenReturn(Optional.of(bookEntity));

        Optional<Book> result = dao.searchBookByTitle(title);

        assertTrue(result.isPresent());
        assertEquals(title, result.get().getTitle());
    }
    @Test
    void test_searchBookByAuthors() {
        String authorName = "Author Test";
        boolean isAvailable = true;

        AuthorEntity authorEntity = new AuthorEntity(UUID.randomUUID(), authorName, "Bio of Author", new HashSet<>());
        Set<AuthorEntity> authors = new HashSet<>();
        authors.add(authorEntity);

        BookEntity bookEntity = new BookEntity(UUID.randomUUID(), "Book Test", "9876543210", 2022, isAvailable, LocalDate.now(), authors, new ArrayList<>());

        when(bookRepository.findBooksByAuthor(authorName, isAvailable)).thenReturn(Collections.singletonList(bookEntity));

        Optional<Book> result = dao.searchBookByAuthors(authorName, isAvailable);

        assertTrue(result.isPresent());
        assertEquals(authorName, result.get().getAuthors().iterator().next().getName());
    }
    @Test
    void test_searchByIsbn() {
        String isbn = "9876543210";
        BookEntity bookEntity = new BookEntity(UUID.randomUUID(), "Book Test", isbn, 2022, true, LocalDate.now(), new HashSet<>(), new ArrayList<>());
        when(bookRepository.findBooksByIsbn(isbn)).thenReturn(Optional.of(bookEntity));

        Optional<Book> result = dao.searchByIsbn(isbn);

        assertTrue(result.isPresent());
        assertEquals(isbn, result.get().getIsbn());
    }
    @Test
    void test_searchBookById() {
        UUID bookId = UUID.randomUUID();
        BookEntity bookEntity = new BookEntity(bookId, "Book Test", "9876543210", 2022, true, LocalDate.now(), new HashSet<>(), new ArrayList<>());
        when(bookRepository.findBookByBookId(bookId)).thenReturn(Optional.of(bookEntity));

        Optional<Book> result = dao.searchBookById(bookId);

        assertTrue(result.isPresent());
        assertEquals(bookId, result.get().getBookId());
    }
    @Test
    void test_searchBooks() {
        String query = "Test";
        Pageable pageable = PageRequest.of(0, 10);
        BookEntity bookEntity = new BookEntity(UUID.randomUUID(), "Book Test", "9876543210", 2022, true, LocalDate.now(), new HashSet<>(), new ArrayList<>());
        Page<BookEntity> page = new PageImpl<>(Collections.singletonList(bookEntity));

        when(bookRepository.findBooksByQuery(query.toLowerCase(), pageable)).thenReturn(page);

        Page<Book> result = dao.searchBooks(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Book Test", result.getContent().getFirst().getTitle());
    }
}
 */