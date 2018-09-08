package com.iandtop.front.smartpark.pos.action;

import com.iandtop.front.smartpark.pos.vo.PosMessage;
import com.iandtop.front.smartpark.pos.vo.ServerMessage;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * @author andyzhao
 */
public interface IPosAction {
    void handle(PosMessage msg, Vertx vertx, Handler<ServerMessage> posServerMessageHandler);
}
