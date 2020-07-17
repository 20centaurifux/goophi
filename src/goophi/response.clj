(ns goophi.response)

(defprotocol Response
  "Response data source."
  (take! [in out]))

(extend java.io.InputStream
  Response
  {:take! #(.read % %2)})

;; Wraps a java.io.FileInputStream and appends a dot to the end.
(deftype TextFile [stream]
  Response
  (take! [in out]
    (try
      (let [buffer-size (alength out)
            available (.available stream)
            taken (.read stream out)]
        (if (or (pos? (.available stream))
                (= taken buffer-size))
          taken
          (let [taken' (max 0 taken)]
            (aset-byte out taken' \.)
            (.close stream)
            (inc taken'))))
      (catch Exception _ -1))))

(defprotocol TextFactory
  "Creates a plain text response."
  (->text [in]))

(extend-protocol TextFactory
  java.lang.String
  (->text [in]
    (java.io.ByteArrayInputStream. (.getBytes (str in \.))))
  java.io.FileInputStream
  (->text [in]
    (TextFile. in)))

(defprotocol BinaryFactory
  "Creates a binary response."
  (->binary [in]))

(extend-protocol BinaryFactory
  (Class/forName "[B")
  (->binary [in]
    (java.io.ByteArrayInputStream. in))
  java.io.InputStream
  (->binary [in]
    in))
