(ns metabase.remote-sync.models.remote-sync-worktree
  "A remote sync worktree, a checkout of a branch whose content lives in the same tables as the main app."
  (:require
   [metabase.models.interface :as mi]
   [metabase.remote-sync.db :as remote-sync.db]
   [methodical.core :as methodical]
   [toucan2.core :as t2]))

(set! *warn-on-reflection* true)

(methodical/defmethod t2/table-name :model/RemoteSyncWorktree [_model] :remote_sync_worktree)

(doto :model/RemoteSyncWorktree
  (derive :metabase/model)
  (derive :hook/timestamped?))

(methodical/defmethod t2/batched-hydrate [:model/RemoteSyncWorktree :users]
  "The users working in each worktree."
  [_model k worktrees]
  (mi/instances-with-hydrated-data
   worktrees k
   #(group-by :worktree_id (remote-sync.db/worktree-users (map :id worktrees)))
   :id
   {:default []}))
