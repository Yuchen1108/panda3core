; Transport two-cities-sequential-30nodes-1000size-4degree-100mindistance-4trucks-20packages-2008seed

(define (problem transport-two-cities-sequential-30nodes-1000size-4degree-100mindistance-4trucks-20packages-2008seed)
 (:domain transport)
 (:objects
  city-1-loc-1 - location
  city-2-loc-1 - location
  city-1-loc-2 - location
  city-2-loc-2 - location
  city-1-loc-3 - location
  city-2-loc-3 - location
  city-1-loc-4 - location
  city-2-loc-4 - location
  city-1-loc-5 - location
  city-2-loc-5 - location
  city-1-loc-6 - location
  city-2-loc-6 - location
  city-1-loc-7 - location
  city-2-loc-7 - location
  city-1-loc-8 - location
  city-2-loc-8 - location
  city-1-loc-9 - location
  city-2-loc-9 - location
  city-1-loc-10 - location
  city-2-loc-10 - location
  city-1-loc-11 - location
  city-2-loc-11 - location
  city-1-loc-12 - location
  city-2-loc-12 - location
  city-1-loc-13 - location
  city-2-loc-13 - location
  city-1-loc-14 - location
  city-2-loc-14 - location
  city-1-loc-15 - location
  city-2-loc-15 - location
  city-1-loc-16 - location
  city-2-loc-16 - location
  city-1-loc-17 - location
  city-2-loc-17 - location
  city-1-loc-18 - location
  city-2-loc-18 - location
  city-1-loc-19 - location
  city-2-loc-19 - location
  city-1-loc-20 - location
  city-2-loc-20 - location
  city-1-loc-21 - location
  city-2-loc-21 - location
  city-1-loc-22 - location
  city-2-loc-22 - location
  city-1-loc-23 - location
  city-2-loc-23 - location
  city-1-loc-24 - location
  city-2-loc-24 - location
  city-1-loc-25 - location
  city-2-loc-25 - location
  city-1-loc-26 - location
  city-2-loc-26 - location
  city-1-loc-27 - location
  city-2-loc-27 - location
  city-1-loc-28 - location
  city-2-loc-28 - location
  city-1-loc-29 - location
  city-2-loc-29 - location
  city-1-loc-30 - location
  city-2-loc-30 - location
  truck-1 - vehicle
  truck-2 - vehicle
  truck-3 - vehicle
  truck-4 - vehicle
  package-1 - package
  package-2 - package
  package-3 - package
  package-4 - package
  package-5 - package
  package-6 - package
  package-7 - package
  package-8 - package
  package-9 - package
  package-10 - package
  package-11 - package
  package-12 - package
  package-13 - package
  package-14 - package
  package-15 - package
  package-16 - package
  package-17 - package
  package-18 - package
  package-19 - package
  package-20 - package
  capacity-0 - capacity-number
  capacity-1 - capacity-number
  capacity-2 - capacity-number
  capacity-3 - capacity-number
  capacity-4 - capacity-number
 )
 (:htn
  :tasks (and
   (deliver package-1 city-2-loc-8)
   (deliver package-2 city-2-loc-14)
   (deliver package-3 city-2-loc-10)
   (deliver package-4 city-2-loc-18)
   (deliver package-5 city-2-loc-29)
   (deliver package-6 city-2-loc-9)
   (deliver package-7 city-2-loc-16)
   (deliver package-8 city-2-loc-15)
   (deliver package-9 city-2-loc-15)
   (deliver package-10 city-2-loc-18)
   (deliver package-11 city-2-loc-26)
   (deliver package-12 city-2-loc-26)
   (deliver package-13 city-2-loc-13)
   (deliver package-14 city-2-loc-3)
   (deliver package-15 city-2-loc-26)
   (deliver package-16 city-2-loc-22)
   (deliver package-17 city-2-loc-17)
   (deliver package-18 city-2-loc-11)
   (deliver package-19 city-2-loc-26)
   (deliver package-20 city-2-loc-17))
  :ordering ( )
  :constraints ( ))
 (:init
  (capacity-predecessor capacity-0 capacity-1)
  (capacity-predecessor capacity-1 capacity-2)
  (capacity-predecessor capacity-2 capacity-3)
  (capacity-predecessor capacity-3 capacity-4)
  (road city-1-loc-4 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-4)
  (road city-1-loc-5 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-5)
  (road city-1-loc-6 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-6)
  (road city-1-loc-7 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-7)
  (road city-1-loc-8 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-8)
  (road city-1-loc-11 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-9)
  (road city-1-loc-9 city-1-loc-11)
  (road city-1-loc-12 city-1-loc-4)
  (road city-1-loc-4 city-1-loc-12)
  (road city-1-loc-14 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-14)
  (road city-1-loc-15 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-15)
  (road city-1-loc-16 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-16)
  (road city-1-loc-16 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-16)
  (road city-1-loc-17 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-17)
  (road city-1-loc-18 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-18)
  (road city-1-loc-19 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-9)
  (road city-1-loc-9 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-19)
  (road city-1-loc-20 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-20)
  (road city-1-loc-21 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-21)
  (road city-1-loc-21 city-1-loc-18)
  (road city-1-loc-18 city-1-loc-21)
  (road city-1-loc-22 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-22)
  (road city-1-loc-23 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-23)
  (road city-1-loc-23 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-23)
  (road city-1-loc-23 city-1-loc-16)
  (road city-1-loc-16 city-1-loc-23)
  (road city-1-loc-24 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-12)
  (road city-1-loc-12 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-16)
  (road city-1-loc-16 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-23)
  (road city-1-loc-23 city-1-loc-24)
  (road city-1-loc-25 city-1-loc-13)
  (road city-1-loc-13 city-1-loc-25)
  (road city-1-loc-26 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-26)
  (road city-1-loc-27 city-1-loc-21)
  (road city-1-loc-21 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-25)
  (road city-1-loc-25 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-27)
  (road city-1-loc-28 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-28)
  (road city-1-loc-28 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-28)
  (road city-1-loc-29 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-29)
  (road city-1-loc-30 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-30)
  (road city-1-loc-30 city-1-loc-9)
  (road city-1-loc-9 city-1-loc-30)
  (road city-1-loc-30 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-30)
  (road city-1-loc-30 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-30)
  (road city-2-loc-3 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-3)
  (road city-2-loc-4 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-4)
  (road city-2-loc-5 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-5)
  (road city-2-loc-7 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-7)
  (road city-2-loc-8 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-8)
  (road city-2-loc-8 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-8)
  (road city-2-loc-10 city-2-loc-9)
  (road city-2-loc-9 city-2-loc-10)
  (road city-2-loc-12 city-2-loc-9)
  (road city-2-loc-9 city-2-loc-12)
  (road city-2-loc-13 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-13)
  (road city-2-loc-14 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-14)
  (road city-2-loc-15 city-2-loc-11)
  (road city-2-loc-11 city-2-loc-15)
  (road city-2-loc-16 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-8)
  (road city-2-loc-8 city-2-loc-16)
  (road city-2-loc-17 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-17)
  (road city-2-loc-17 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-17)
  (road city-2-loc-18 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-11)
  (road city-2-loc-11 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-13)
  (road city-2-loc-13 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-15)
  (road city-2-loc-15 city-2-loc-18)
  (road city-2-loc-19 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-19)
  (road city-2-loc-19 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-19)
  (road city-2-loc-20 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-20)
  (road city-2-loc-21 city-2-loc-9)
  (road city-2-loc-9 city-2-loc-21)
  (road city-2-loc-21 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-21)
  (road city-2-loc-22 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-22)
  (road city-2-loc-23 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-4)
  (road city-2-loc-4 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-23)
  (road city-2-loc-24 city-2-loc-13)
  (road city-2-loc-13 city-2-loc-24)
  (road city-2-loc-24 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-24)
  (road city-2-loc-24 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-24)
  (road city-2-loc-25 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-13)
  (road city-2-loc-13 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-24)
  (road city-2-loc-24 city-2-loc-25)
  (road city-2-loc-26 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-26)
  (road city-2-loc-26 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-26)
  (road city-2-loc-27 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-4)
  (road city-2-loc-4 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-17)
  (road city-2-loc-17 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-27)
  (road city-2-loc-28 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-28)
  (road city-2-loc-28 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-28)
  (road city-2-loc-28 city-2-loc-26)
  (road city-2-loc-26 city-2-loc-28)
  (road city-2-loc-29 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-29)
  (road city-2-loc-29 city-2-loc-8)
  (road city-2-loc-8 city-2-loc-29)
  (road city-2-loc-29 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-29)
  (road city-2-loc-29 city-2-loc-22)
  (road city-2-loc-22 city-2-loc-29)
  (road city-2-loc-30 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-30)
  (road city-2-loc-30 city-2-loc-19)
  (road city-2-loc-19 city-2-loc-30)
  (road city-2-loc-30 city-2-loc-21)
  (road city-2-loc-21 city-2-loc-30)
  (road city-1-loc-25 city-2-loc-4)
  (road city-2-loc-4 city-1-loc-25)
  (at package-1 city-1-loc-2)
  (at package-2 city-1-loc-17)
  (at package-3 city-1-loc-26)
  (at package-4 city-1-loc-1)
  (at package-5 city-1-loc-5)
  (at package-6 city-1-loc-13)
  (at package-7 city-1-loc-2)
  (at package-8 city-1-loc-25)
  (at package-9 city-1-loc-29)
  (at package-10 city-1-loc-6)
  (at package-11 city-1-loc-21)
  (at package-12 city-1-loc-24)
  (at package-13 city-1-loc-3)
  (at package-14 city-1-loc-11)
  (at package-15 city-1-loc-26)
  (at package-16 city-1-loc-14)
  (at package-17 city-1-loc-25)
  (at package-18 city-1-loc-25)
  (at package-19 city-1-loc-27)
  (at package-20 city-1-loc-10)
  (at truck-1 city-2-loc-9)
  (capacity truck-1 capacity-3)
  (at truck-2 city-2-loc-23)
  (capacity truck-2 capacity-2)
  (at truck-3 city-2-loc-9)
  (capacity truck-3 capacity-2)
  (at truck-4 city-2-loc-27)
  (capacity truck-4 capacity-4)
 )
)
