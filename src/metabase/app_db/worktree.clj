(ns metabase.app-db.worktree
  "The remote sync worktree the app DB connections of a thread see. On Postgres, row-level security shows a connection
  only the rows of the worktree its `metabase.worktree_id` setting names, `0` for the main app, or every row when the
  setting is empty or unset.")

(set! *warn-on-reflection* true)

#_{:clj-kondo/ignore [:metabase/discourage-dynamic-vars]}
(def ^:dynamic *worktree-id*
  "The id of the worktree the app DB connections of this thread see, nil for the main app, or `:all` for every
  worktree."
  nil)

(defn worktree-id
  "The id of the worktree this thread works in, or nil in the main app or across every worktree."
  []
  (when (int? *worktree-id*)
    *worktree-id*))

(defn do-with-worktree-id
  "Impl for [[with-worktree-id]]."
  [worktree-id thunk]
  (binding [*worktree-id* worktree-id]
    (thunk)))

(defmacro with-worktree-id
  "Runs `body` with the app DB connections it gets seeing the worktree with `worktree-id`, nil for the main app, or
  `:all` for every worktree."
  {:style/indent 1}
  [worktree-id & body]
  `(do-with-worktree-id ~worktree-id (^:once fn* [] ~@body)))

(def main-app-sql
  "The statement that puts a Postgres app DB connection in the main app."
  "SET metabase.worktree_id = '0';")

(def check-in-sql
  "The statement a Postgres app DB connection runs at check-in, off the thread that used it: what `DISCARD ALL` does,
  which can't run with other statements in one, then [[main-app-sql]]."
  (str "CLOSE ALL; SET SESSION AUTHORIZATION DEFAULT; RESET ALL; DEALLOCATE ALL; UNLISTEN *; "
       "SELECT pg_advisory_unlock_all(); DISCARD PLANS; DISCARD TEMP; DISCARD SEQUENCES; "
       main-app-sql))

(defn checkout-sql
  "The statement a Postgres app DB connection, in the main app since check-in, runs when this thread checks it out, or
  nil when this thread works in the main app."
  []
  (cond
    (nil? *worktree-id*)    nil
    (= :all *worktree-id*)  "RESET metabase.worktree_id;"
    :else                   (format "SET metabase.worktree_id = '%d';" (long *worktree-id*))))
