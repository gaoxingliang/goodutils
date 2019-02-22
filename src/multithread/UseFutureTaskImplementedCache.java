package multithread;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

/**
 * 使用FutureTask来实现的高效缓存.
 */
public class UseFutureTaskImplementedCache {

    class AccountCache {
        private final ConcurrentHashMap<String, FutureTask<Account>> _cache;

        public AccountCache() {
            this._cache = new ConcurrentHashMap<>();
        }

        public Account get(String id) throws Exception {
            FutureTask<Account> exists = _cache.get(id);
            if (exists != null) {
                // 结果已经存在或者正有人在计算
                return exists.get();
            }

            FutureTask<Account> newTask = new FutureTask<Account>(new Callable<Account>() {
                @Override
                public Account call() throws Exception {
                    return _loadFromDB(id);
                }
            });

            exists = _cache.putIfAbsent(id, newTask);
            if (exists == null) {
                // let me call it
                newTask.run();
                return newTask.get();
            } else {
                // someone has doing cal
                return exists.get();
            }

        }

        Account _loadFromDB(String id){
            //load it...
            return null;
        }

    }


    class Account {

    }



}
