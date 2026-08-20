import type { Author, Book, Customer, Transaction } from '../../types/domain'

/**
 * The demo's data, and the rules the real backend would enforce.
 *
 * Everything lives in localStorage under one key, so a visitor's library survives a page reload but
 * belongs only to their browser. Clearing site data resets it, which is the intended escape hatch.
 */

const KEY = 'library.demo.state'
const LOAN_DAYS = 28
export const LOAN_LIMIT = 5

export interface DemoUser {
  username: string
  password: string
  role: 'ADMIN' | 'USER'
  customerId?: string
}

export interface DemoState {
  users: DemoUser[]
  customers: Customer[]
  books: Book[]
  authors: Author[]
  transactions: Transaction[]
  reminders: Record<string, boolean>
}

const uuid = () =>
  typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`

const iso = (d: Date) => d.toISOString().slice(0, 10)

function author(name: string, bio: string): Author {
  return { authorId: uuid(), name, bio }
}

/** Enough of a catalogue that paging, sorting and searching are worth trying. */
const SEED: [string, string, number, string, string][] = [
  ['The Left Hand of Darkness', '978-0-441-47812-5', 1969, 'Ursula K. Le Guin', 'A envoy to a world without fixed gender.'],
  ['A Wizard of Earthsea', '978-0-553-38304-1', 1968, 'Ursula K. Le Guin', 'A boy with a true name and a shadow behind him.'],
  ['Dune', '978-0-441-01359-3', 1965, 'Frank Herbert', 'Spice, sand, and a very long game.'],
  ['One Hundred Years of Solitude', '978-0-06-088328-7', 1967, 'Gabriel Garcia Marquez', 'Seven generations in Macondo.'],
  ['Gödel, Escher, Bach', '978-0-465-02656-2', 1979, 'Douglas Hofstadter', 'Strange loops, and how minds might be one.'],
  ['A Brief History of Time', '978-0-553-38016-3', 1988, 'Stephen Hawking', 'Cosmology without the equations.'],
  ['The Name of the Rose', '978-0-15-600131-7', 1980, 'Umberto Eco', 'A murder in an abbey with a dangerous library.'],
  ['Beloved', '978-1-4000-3341-6', 1987, 'Toni Morrison', 'A house haunted by what slavery did.'],
  ['Things Fall Apart', '978-0-385-47454-2', 1958, 'Chinua Achebe', 'Okonkwo, and the world arriving to end his.'],
  ['The Master and Margarita', '978-0-14-118014-8', 1967, 'Mikhail Bulgakov', 'The devil visits Moscow, with a cat.'],
  ['Invisible Cities', '978-0-15-645380-2', 1972, 'Italo Calvino', 'Marco Polo describes cities that may be one city.'],
  ['The Dispossessed', '978-0-06-051275-4', 1974, 'Ursula K. Le Guin', 'Two worlds, one wall, and a physicist between them.'],
  ['Never Let Me Go', '978-1-4000-7877-6', 2005, 'Kazuo Ishiguro', 'A boarding school, remembered too fondly.'],
  ['The Remains of the Day', '978-0-679-73172-6', 1989, 'Kazuo Ishiguro', 'A butler counts the cost of his own discretion.'],
  ['Piranesi', '978-1-63557-563-4', 2020, 'Susanna Clarke', 'A house of endless halls, and the person who lives there.'],
  ['Station Eleven', '978-0-8041-7244-8', 2014, 'Emily St. John Mandel', 'A travelling orchestra after the collapse.'],
  ['The Overstory', '978-0-393-63552-2', 2018, 'Richard Powers', 'Nine people, and the trees that outlast them.'],
  ['Educated', '978-0-399-59050-4', 2018, 'Tara Westover', 'A childhood off the grid, and the way out.'],
  ['Sapiens', '978-0-06-231609-7', 2011, 'Yuval Noah Harari', 'How one ape came to run the place.'],
  ['The Sixth Extinction', '978-1-250-06218-5', 2014, 'Elizabeth Kolbert', 'The one happening now, and who is causing it.'],
  ['Thinking, Fast and Slow', '978-0-374-53355-7', 2011, 'Daniel Kahneman', 'Two systems, and how the quick one fools you.'],
  ['The Goldfinch', '978-0-316-05543-7', 2013, 'Donna Tartt', 'A boy, a bomb, and a small Dutch painting.'],
  ['Wolf Hall', '978-0-312-42998-0', 2009, 'Hilary Mantel', 'Thomas Cromwell, from the inside.'],
  ['The Road', '978-0-307-38789-9', 2006, 'Cormac McCarthy', 'A man and his son, walking south.'],
]

function seed(): DemoState {
  const authors = new Map<string, Author>()
  const books: Book[] = SEED.map(([title, isbn, year, who, description]) => {
    if (!authors.has(who)) authors.set(who, author(who, 'Writer.'))
    return {
      bookId: uuid(),
      title,
      isbn,
      publicationYear: year,
      createdAt: iso(new Date()),
      authors: [authors.get(who)!],
      available: true,
      description,
    }
  })

  const ada: Customer = { customerId: uuid(), name: 'Ada Lovelace', email: 'ada@example.com', privileges: true }
  const alan: Customer = { customerId: uuid(), name: 'Alan Turing', email: 'alan@example.com', privileges: true }

  return {
    // The administrator exists from the start, exactly as DataInitializer creates it.
    users: [
      { username: 'admin', password: 'admin', role: 'ADMIN' },
      { username: 'ada', password: 'ada', role: 'USER', customerId: ada.customerId },
    ],
    customers: [ada, alan],
    books,
    authors: [...authors.values()],
    transactions: [],
    reminders: {},
  }
}

let state: DemoState | null = null

export function db(): DemoState {
  if (state) return state
  try {
    const raw = localStorage.getItem(KEY)
    state = raw ? (JSON.parse(raw) as DemoState) : seed()
  } catch {
    state = seed()
  }
  return state!
}

export function save(): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(db()))
  } catch {
    // A full or blocked storage quota is not worth failing a demo over.
  }
}

export function reset(): void {
  state = seed()
  save()
}

export const helpers = {
  uuid,
  iso,
  dueDateFrom: (from: Date) => iso(new Date(from.getTime() + LOAN_DAYS * 86_400_000)),
  activeLoansFor: (customerId: string) =>
    db().transactions.filter((t) => t.customerId === customerId && !t.returnDate),
  openLoanForBook: (bookId: string) =>
    db().transactions.find((t) => t.bookId === bookId && !t.returnDate),
  customer: (id?: string) => db().customers.find((c) => c.customerId === id),
  book: (id: string) => db().books.find((b) => b.bookId === id),
}
