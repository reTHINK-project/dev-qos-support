/**
 ******************************************************************************
 * @b Project : reThink
 *
 * @b Sub-project : QoS Broker
 *
 ******************************************************************************
 *
 *                       Copyright (C) 2016 Orange Labs
 *                       Orange Labs - PROPRIETARY
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. *
 ******************************************************************************
 *
 * @brief API of the extended features drawn for the broker project
 *
 * @file
 *
 */

#ifndef __BROKER_H__
#define __BROKER_H__

#include "ns_turn_ioalib.h"
#include "mainrelay.h"


#ifdef __cplusplus
extern "C" {
#endif

#if !defined(true)
#define true (0==0)
#endif

#if !defined(false)
#define false (!true)
#endif

#if !defined(bool)
#define bool int
#endif

/**
 * Timeout value used to register / refresh periodically the turn server inside the DB
 * Value in seconds
 * @TODO Should be part of the configuration file
 */
#define TURN_REGISTER_REFRESH_PERIOD     60

/**
 * TTL (Time To Live) value of the registered turn key inside the DB
 * Value MUST be greater than the refresh period to insure that it always exists
 * in DB... in a normal behavior
 */
#define TURN_REGISTERED_TTL_VALUE        (TURN_REGISTER_REFRESH_PERIOD + 5)

#define MAX_DB_TURN_REGISTER_KEY       1024


typedef struct _broker_ctx_t
{
    char                    db_turn_register_key[MAX_DB_TURN_REGISTER_KEY] ;
    pthread_t               thr ;
    struct event_base *     event_base ;
    redis_context_handle    rch ;

} broker_ctx_t ;


/**
 * Setup and init the broker server.
 * This is the main entry point to initialize all communications between broker,
 * database and current turn server
 *
 * @return N/A
 */
void setup_broker_server(void) ;


#ifdef __cplusplus
}
#endif

#endif
