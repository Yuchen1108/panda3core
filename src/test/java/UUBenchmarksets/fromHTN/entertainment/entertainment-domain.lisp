; to enable unplugging, the overall task might be modeled as follows
; ordered:
; - unplug and dump broken cables
; - unplug if necessary
; - re-plug

; - need to delete a connect task if there is already a connection (methods are given, but commented, we need conditional effects to do that)
; - we might force that one of the devices involved in a plug action is a cable
; - compatibility might be solved via the type of the connector
; - test auf Ungleichheit einbauen
; - is it possible to do crazy mixed-in/out-things with an scart-to-cinch-cable?
; - make a totally-ordered version

(define (domain entertainment)
  (:requirements :typing)
  (:types
      equipment connector - object
  )

  (:predicates
     (audio_connected ?e1 ?e2 - equipment)
     (audio_connector ?c - connector)
     (compatible ?c1 ?c2 - connector)
     (conn_of ?e - equipment ?c - connector)
     (in_connector ?c - connector)
     (out_connector ?c - connector)
     (unused ?c - connector)
     (video_connected ?e1 ?e2 - equipment)
     (video_connector ?c - connector)
  )
  
  (:task a_connect :parameters (?e1 ?e2 - equipment))
  (:task v_connect :parameters (?e1 ?e2 - equipment))
  (:task av_connect :parameters (?e1 ?e2 - equipment))
  (:task direct_a_connect :parameters (?e1 ?e2 - equipment))
  (:task direct_v_connect :parameters (?e1 ?e2 - equipment))
  (:task direct_av_connect :parameters (?e1 ?e2 - equipment))

; av connections----------------------------------------------------------

  ; direct av connection
  (:method m-connect-direct-av
    :parameters (?e1 ?e2 - equipment)
    :task (av_connect ?e1 ?e2)
    :subtasks (direct_av_connect ?e1 ?e2)
  )

  ; indirect av connection
  (:method m-connect-rec-av-1
    :parameters (?e1 ?e2 ?e3 - equipment)
    :task (av_connect ?e1 ?e3)
    :subtasks (and
        (direct_av_connect ?e1 ?e2)
        (av_connect ?e2 ?e3))
  )

  ; indirect av connection
  (:method m-connect-rec-av-2
    :parameters (?e1 ?e2 ?e3 - equipment)
    :task (av_connect ?e1 ?e3)
    :subtasks (and
        (av_connect ?e1 ?e2)
        (direct_av_connect ?e2 ?e3))
  )

  ; indirect av with split (no join possible anymore)
  (:method m-connect-split
    :parameters (?e1 ?e2 - equipment)
    :task (av_connect ?e1 ?e2)
    :subtasks (and
        (v_connect ?e1 ?e2)
        (a_connect ?e1 ?e2))
  )

; a connections----------------------------------------------------------

  ; direct a connection
  (:method m-connect-direct-a
    :parameters (?e1 ?e2 - equipment)
    :task (a_connect ?e1 ?e2)
    :subtasks (direct_a_connect ?e1 ?e2)
  )

  ; indirect a connection
  (:method m-connect-rec-a
    :parameters (?e1 ?e2 ?e3 - equipment)
    :task (a_connect ?e1 ?e3)
    :subtasks (and
        (a_connect ?e1 ?e2)
        (direct_a_connect ?e2 ?e3))
  )

; v connections----------------------------------------------------------

  ; direct v connection
  (:method m-connect-direct-v
    :parameters (?e1 ?e2 - equipment)
    :task (v_connect ?e1 ?e2)
    :subtasks (direct_v_connect ?e1 ?e2)
  )

  ; indirect v connection
  (:method m-connect-rec-v
    :parameters (?e1 ?e2 ?e3 - equipment)
    :task (v_connect ?e1 ?e3)
    :subtasks (and
        (v_connect ?e1 ?e2)
        (direct_v_connect ?e2 ?e3))
  )

; direct connections------------------------------------------------------

  (:method m-dconnect-av
    :parameters (?e1 ?e2 - equipment ?c1 ?c2 - connector)
    :task (direct_av_connect ?e1 ?e2)
    :precondition (and
        (not (= ?e1 ?e2))
        (audio_connector ?c1)
        (audio_connector ?c2)
        (video_connector ?c1)
        (video_connector ?c2)
        (out_connector ?c1)
        (in_connector ?c2)
      )
    :subtasks (and
        (plug ?e1 ?c1 ?e2 ?c2))
  )

  (:method m-dconnect-a
    :parameters (?e1 ?e2 - equipment ?c1 ?c2 - connector)
    :task (direct_a_connect ?e1 ?e2)
    :precondition (and
        (not (= ?e1 ?e2))
        (audio_connector ?c1)
        (audio_connector ?c2)
        (out_connector ?c1)
        (in_connector ?c2)
      )
    :subtasks (and
        (plug ?e1 ?c1 ?e2 ?c2))
  )

  (:method m-dconnect-v
    :parameters (?e1 ?e2 - equipment ?c1 ?c2 - connector)
    :task (direct_v_connect ?e1 ?e2)
    :precondition (and
        ;;(not (= ?pl1 ?pl2))
        (not (= ?e1 ?e2))
        (video_connector ?c1)
        (video_connector ?c2)
        (out_connector ?c1)
        (in_connector ?c2)
      )
    :subtasks (and
        (plug ?e1 ?c1 ?e2 ?c2))
  )

; connections via existing cable -----------------------------------------

  (:method m-dconnect-av-empty
    :parameters (?e1 ?e2 - equipment)
    :task (direct_av_connect ?e1 ?e2)
    :precondition (and
        (audio_connected ?e1 ?e2)
        (video_connected ?e1 ?e2)
      )
    :subtasks ( )
  )

  (:method m-dconnect-a-empty
    :parameters (?e1 ?e2 - equipment )
    :task (direct_a_connect ?e1 ?e2)
    :precondition (and
        (audio_connected ?e1 ?e2)
      )
    :subtasks ( )
  )

  (:method m-dconnect-v-empty
    :parameters (?e1 ?e2 - equipment)
    :task (direct_v_connect ?e1 ?e2)
    :precondition (and
        (video_connected ?e1 ?e2)
      )
    :subtasks ( )
  )

; primitives -------------------------------------------------------------
  
  (:action plug
    :parameters (?e1 - equipment ?c1 - connector
                 ?e2 - equipment ?c2 - connector)
    :precondition (and
        (unused ?c1)
        (unused ?c2)
        (conn_of ?e1 ?c1)
        (conn_of ?e2 ?c2)
        (compatible ?c1 ?c2)
      )
    :effect (and
        (when (and (audio_connector ?c1)
                   (audio_connector ?c2))
              (audio_connected ?e1 ?e2))
        (when (and (video_connector ?c1)
                   (video_connector ?c2))
              (video_connected ?e1 ?e2))
        (not (unused ?c1))(not (unused ?c2)))
      )
  )
)
;;   (:action unplug
;;     :parameters (?e1 ?e2 - device ?c1 ?c2 - port
;;                  ?c - cable ?pl1 ?pl2 - plug)
;;     :precondition (and
;;         (plugged ?c1 ?c2 ?c)
;;         (port_of ?e1 ?c1)
;;         (port_of ?e2 ?c2)
;;         (plug_of ?c ?pl1)
;;         (plug_of ?c ?pl2)
;;         (compatible ?c1 ?pl1)
;;         (compatible ?c2 ?pl2)
;;       )
;;     :effect (and
;;         (not (audio_connected ?e1 ?e2))
;;         (not (video_connected ?e1 ?e2))
;; 
;;         (when (exists <so wie oben plus>
;;                    (audio_port ?e1)
;;                    (audio_port ?e2)
;;                    (audio_cable ?fc))
;;               (audio_connected ?e1 ?e2))
;;         (when (exists <so wie oben plus>
;;                    (video_port ?e1)
;;                    (video_port ?e2)
;;                    (video_cable ?c))
;;               (video_connected ?e1 ?e2))
;;       )
;;   )
