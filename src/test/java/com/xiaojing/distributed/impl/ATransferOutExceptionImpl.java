package com.xiaojing.distributed.impl;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.xiaojing.distributed.server.SimpleTransfer;
import com.xiaojing.distributed.util.Context;
import com.xiaojing.distributed.annotation.DistributeJob;
import com.xiaojing.distributed.annotation.DoJob;
import com.xiaojing.distributed.annotation.GetConnection;
import com.xiaojing.distributed.annotation.SharedKey;
import com.xiaojing.distributed.annotation.UndoJob;
import com.xiaojing.distributed.dao.DistributeJobDao;
import com.xiaojing.distributed.dao.UndoJobDao;
import com.xiaojing.distributed.dao.UserDao;
import com.xiaojing.distributed.interceptor.DistributeJobInterceptor;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * throw exception before A transferOut
 * use TransferOutDoJob and TransferInDoJob replace DoJob
 */
@Singleton
public class ATransferOutExceptionImpl implements SimpleTransfer.Iface {

  private static final Logger LOGGER = LoggerFactory.getLogger(ATransferOutExceptionImpl.class);
  private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
  @Inject
  private DistributeJobDao distributeJobDao;
  @Inject
  private UserDao userDao;
  @Inject
  private UndoJobDao undoJobDao;
  @Inject
  private DistributeJobInterceptor distributeJobInterceptor;

  @Override
  public boolean transfer(String fromId, String toId, long amount) throws TException {
    return transfer(null, fromId, toId, amount);
  }

  @DistributeJob
  public boolean transfer(Context context, String fromId, String toId, long amount)
      throws TException {
    String transferId = UUID.randomUUID().toString();
    try {
      transferOut(context, transferId, fromId, toId, amount);
      transferIn(context, transferId, fromId, toId, amount);
    } catch (Exception e) {
      throw new TException();
    }
    return true;
  }

  @UndoJob
  @GetConnection
  public boolean undoTransferIn(Context context, com.xiaojing.distributed.model.UndoJob undoJob,
                                String transferId, String fromId, @SharedKey("userId") String toId,
                                long amount) throws Exception {
    userDao.updateBalanceById(context.getConnection(), toId, -amount, Long.MIN_VALUE);
    return true;
  }

  @UndoJob
  @GetConnection
  public boolean undoTransferOut(Context context, com.xiaojing.distributed.model.UndoJob undoJob,
                                 String transferId, @SharedKey("userId") String fromId, String toId,
                                 long amount) throws Exception {
    userDao.updateBalanceById(context.getConnection(), fromId, amount, Long.MIN_VALUE);
    return true;
  }

  @DoJob
  @GetConnection
  public boolean transferOut(Context context, String transferId, @SharedKey("userId") String fromId,
                             String toId, long amount) throws Exception {
    LOGGER.warn("throw Exception here,before transfer out success");
    throw new RuntimeException();
        /*userDao.updateBalanceById(context.getConnection(),fromId, -amount, 0L);
        undoTransferOut(context, null, transferId, fromId, toId, amount);
        return true;*/
  }

  @DoJob
  @GetConnection
  public boolean transferIn(Context context, String transferId, String fromId,
                            @SharedKey("userId") String toId, long amount) throws Exception {
    userDao.updateBalanceById(context.getConnection(), toId, amount, Long.MIN_VALUE);
    undoTransferIn(context, null, transferId, fromId, toId, amount);
    return true;
  }

}
