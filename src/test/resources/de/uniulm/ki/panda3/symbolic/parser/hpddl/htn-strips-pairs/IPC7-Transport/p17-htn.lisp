; Transport three-cities-sequential-60nodes-1000size-4degree-100mindistance-4trucks-22packages-2013seed

(define (problem transport-three-cities-sequential-60nodes-1000size-4degree-100mindistance-4trucks-22packages-2013seed)
 (:domain transport)
 (:objects
  city-1-loc-1 - location
  city-2-loc-1 - location
  city-3-loc-1 - location
  city-1-loc-2 - location
  city-2-loc-2 - location
  city-3-loc-2 - location
  city-1-loc-3 - location
  city-2-loc-3 - location
  city-3-loc-3 - location
  city-1-loc-4 - location
  city-2-loc-4 - location
  city-3-loc-4 - location
  city-1-loc-5 - location
  city-2-loc-5 - location
  city-3-loc-5 - location
  city-1-loc-6 - location
  city-2-loc-6 - location
  city-3-loc-6 - location
  city-1-loc-7 - location
  city-2-loc-7 - location
  city-3-loc-7 - location
  city-1-loc-8 - location
  city-2-loc-8 - location
  city-3-loc-8 - location
  city-1-loc-9 - location
  city-2-loc-9 - location
  city-3-loc-9 - location
  city-1-loc-10 - location
  city-2-loc-10 - location
  city-3-loc-10 - location
  city-1-loc-11 - location
  city-2-loc-11 - location
  city-3-loc-11 - location
  city-1-loc-12 - location
  city-2-loc-12 - location
  city-3-loc-12 - location
  city-1-loc-13 - location
  city-2-loc-13 - location
  city-3-loc-13 - location
  city-1-loc-14 - location
  city-2-loc-14 - location
  city-3-loc-14 - location
  city-1-loc-15 - location
  city-2-loc-15 - location
  city-3-loc-15 - location
  city-1-loc-16 - location
  city-2-loc-16 - location
  city-3-loc-16 - location
  city-1-loc-17 - location
  city-2-loc-17 - location
  city-3-loc-17 - location
  city-1-loc-18 - location
  city-2-loc-18 - location
  city-3-loc-18 - location
  city-1-loc-19 - location
  city-2-loc-19 - location
  city-3-loc-19 - location
  city-1-loc-20 - location
  city-2-loc-20 - location
  city-3-loc-20 - location
  city-1-loc-21 - location
  city-2-loc-21 - location
  city-3-loc-21 - location
  city-1-loc-22 - location
  city-2-loc-22 - location
  city-3-loc-22 - location
  city-1-loc-23 - location
  city-2-loc-23 - location
  city-3-loc-23 - location
  city-1-loc-24 - location
  city-2-loc-24 - location
  city-3-loc-24 - location
  city-1-loc-25 - location
  city-2-loc-25 - location
  city-3-loc-25 - location
  city-1-loc-26 - location
  city-2-loc-26 - location
  city-3-loc-26 - location
  city-1-loc-27 - location
  city-2-loc-27 - location
  city-3-loc-27 - location
  city-1-loc-28 - location
  city-2-loc-28 - location
  city-3-loc-28 - location
  city-1-loc-29 - location
  city-2-loc-29 - location
  city-3-loc-29 - location
  city-1-loc-30 - location
  city-2-loc-30 - location
  city-3-loc-30 - location
  city-1-loc-31 - location
  city-2-loc-31 - location
  city-3-loc-31 - location
  city-1-loc-32 - location
  city-2-loc-32 - location
  city-3-loc-32 - location
  city-1-loc-33 - location
  city-2-loc-33 - location
  city-3-loc-33 - location
  city-1-loc-34 - location
  city-2-loc-34 - location
  city-3-loc-34 - location
  city-1-loc-35 - location
  city-2-loc-35 - location
  city-3-loc-35 - location
  city-1-loc-36 - location
  city-2-loc-36 - location
  city-3-loc-36 - location
  city-1-loc-37 - location
  city-2-loc-37 - location
  city-3-loc-37 - location
  city-1-loc-38 - location
  city-2-loc-38 - location
  city-3-loc-38 - location
  city-1-loc-39 - location
  city-2-loc-39 - location
  city-3-loc-39 - location
  city-1-loc-40 - location
  city-2-loc-40 - location
  city-3-loc-40 - location
  city-1-loc-41 - location
  city-2-loc-41 - location
  city-3-loc-41 - location
  city-1-loc-42 - location
  city-2-loc-42 - location
  city-3-loc-42 - location
  city-1-loc-43 - location
  city-2-loc-43 - location
  city-3-loc-43 - location
  city-1-loc-44 - location
  city-2-loc-44 - location
  city-3-loc-44 - location
  city-1-loc-45 - location
  city-2-loc-45 - location
  city-3-loc-45 - location
  city-1-loc-46 - location
  city-2-loc-46 - location
  city-3-loc-46 - location
  city-1-loc-47 - location
  city-2-loc-47 - location
  city-3-loc-47 - location
  city-1-loc-48 - location
  city-2-loc-48 - location
  city-3-loc-48 - location
  city-1-loc-49 - location
  city-2-loc-49 - location
  city-3-loc-49 - location
  city-1-loc-50 - location
  city-2-loc-50 - location
  city-3-loc-50 - location
  city-1-loc-51 - location
  city-2-loc-51 - location
  city-3-loc-51 - location
  city-1-loc-52 - location
  city-2-loc-52 - location
  city-3-loc-52 - location
  city-1-loc-53 - location
  city-2-loc-53 - location
  city-3-loc-53 - location
  city-1-loc-54 - location
  city-2-loc-54 - location
  city-3-loc-54 - location
  city-1-loc-55 - location
  city-2-loc-55 - location
  city-3-loc-55 - location
  city-1-loc-56 - location
  city-2-loc-56 - location
  city-3-loc-56 - location
  city-1-loc-57 - location
  city-2-loc-57 - location
  city-3-loc-57 - location
  city-1-loc-58 - location
  city-2-loc-58 - location
  city-3-loc-58 - location
  city-1-loc-59 - location
  city-2-loc-59 - location
  city-3-loc-59 - location
  city-1-loc-60 - location
  city-2-loc-60 - location
  city-3-loc-60 - location
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
  package-21 - package
  package-22 - package
  capacity-0 - capacity-number
  capacity-1 - capacity-number
  capacity-2 - capacity-number
  capacity-3 - capacity-number
  capacity-4 - capacity-number
 )
 (:htn
  :tasks (and
   (deliver package-1 city-1-loc-16)
   (deliver package-2 city-3-loc-2)
   (deliver package-3 city-3-loc-25)
   (deliver package-4 city-1-loc-7)
   (deliver package-5 city-2-loc-44)
   (deliver package-6 city-3-loc-7)
   (deliver package-7 city-1-loc-24)
   (deliver package-8 city-2-loc-40)
   (deliver package-9 city-1-loc-19)
   (deliver package-10 city-3-loc-38)
   (deliver package-11 city-1-loc-4)
   (deliver package-12 city-3-loc-36)
   (deliver package-13 city-3-loc-18)
   (deliver package-14 city-2-loc-5)
   (deliver package-15 city-1-loc-57)
   (deliver package-16 city-2-loc-40)
   (deliver package-17 city-1-loc-58)
   (deliver package-18 city-3-loc-42)
   (deliver package-19 city-3-loc-27)
   (deliver package-20 city-3-loc-51)
   (deliver package-21 city-1-loc-1)
   (deliver package-22 city-1-loc-13))
  :ordering ( )
  :constraints ( ))
 (:init
  (capacity-predecessor capacity-0 capacity-1)
  (capacity-predecessor capacity-1 capacity-2)
  (capacity-predecessor capacity-2 capacity-3)
  (capacity-predecessor capacity-3 capacity-4)
  (road city-1-loc-5 city-1-loc-4)
  (road city-1-loc-4 city-1-loc-5)
  (road city-1-loc-9 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-9)
  (road city-1-loc-12 city-1-loc-9)
  (road city-1-loc-9 city-1-loc-12)
  (road city-1-loc-12 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-12)
  (road city-1-loc-13 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-13)
  (road city-1-loc-14 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-14)
  (road city-1-loc-15 city-1-loc-5)
  (road city-1-loc-5 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-9)
  (road city-1-loc-9 city-1-loc-15)
  (road city-1-loc-16 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-16)
  (road city-1-loc-17 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-17)
  (road city-1-loc-18 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-18)
  (road city-1-loc-19 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-19)
  (road city-1-loc-20 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-20)
  (road city-1-loc-22 city-1-loc-13)
  (road city-1-loc-13 city-1-loc-22)
  (road city-1-loc-23 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-23)
  (road city-1-loc-23 city-1-loc-12)
  (road city-1-loc-12 city-1-loc-23)
  (road city-1-loc-24 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-24)
  (road city-1-loc-26 city-1-loc-4)
  (road city-1-loc-4 city-1-loc-26)
  (road city-1-loc-27 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-13)
  (road city-1-loc-13 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-27)
  (road city-1-loc-28 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-28)
  (road city-1-loc-28 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-28)
  (road city-1-loc-31 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-31)
  (road city-1-loc-32 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-32)
  (road city-1-loc-32 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-32)
  (road city-1-loc-33 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-33)
  (road city-1-loc-33 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-33)
  (road city-1-loc-33 city-1-loc-21)
  (road city-1-loc-21 city-1-loc-33)
  (road city-1-loc-34 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-34)
  (road city-1-loc-35 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-35)
  (road city-1-loc-36 city-1-loc-16)
  (road city-1-loc-16 city-1-loc-36)
  (road city-1-loc-37 city-1-loc-4)
  (road city-1-loc-4 city-1-loc-37)
  (road city-1-loc-37 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-37)
  (road city-1-loc-37 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-37)
  (road city-1-loc-37 city-1-loc-32)
  (road city-1-loc-32 city-1-loc-37)
  (road city-1-loc-38 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-38)
  (road city-1-loc-38 city-1-loc-18)
  (road city-1-loc-18 city-1-loc-38)
  (road city-1-loc-39 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-39)
  (road city-1-loc-39 city-1-loc-30)
  (road city-1-loc-30 city-1-loc-39)
  (road city-1-loc-39 city-1-loc-35)
  (road city-1-loc-35 city-1-loc-39)
  (road city-1-loc-40 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-40)
  (road city-1-loc-40 city-1-loc-26)
  (road city-1-loc-26 city-1-loc-40)
  (road city-1-loc-40 city-1-loc-31)
  (road city-1-loc-31 city-1-loc-40)
  (road city-1-loc-41 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-41)
  (road city-1-loc-41 city-1-loc-30)
  (road city-1-loc-30 city-1-loc-41)
  (road city-1-loc-41 city-1-loc-39)
  (road city-1-loc-39 city-1-loc-41)
  (road city-1-loc-42 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-42)
  (road city-1-loc-42 city-1-loc-31)
  (road city-1-loc-31 city-1-loc-42)
  (road city-1-loc-42 city-1-loc-34)
  (road city-1-loc-34 city-1-loc-42)
  (road city-1-loc-42 city-1-loc-40)
  (road city-1-loc-40 city-1-loc-42)
  (road city-1-loc-43 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-43)
  (road city-1-loc-43 city-1-loc-34)
  (road city-1-loc-34 city-1-loc-43)
  (road city-1-loc-44 city-1-loc-13)
  (road city-1-loc-13 city-1-loc-44)
  (road city-1-loc-44 city-1-loc-18)
  (road city-1-loc-18 city-1-loc-44)
  (road city-1-loc-44 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-44)
  (road city-1-loc-44 city-1-loc-38)
  (road city-1-loc-38 city-1-loc-44)
  (road city-1-loc-45 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-45)
  (road city-1-loc-45 city-1-loc-14)
  (road city-1-loc-14 city-1-loc-45)
  (road city-1-loc-45 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-45)
  (road city-1-loc-45 city-1-loc-28)
  (road city-1-loc-28 city-1-loc-45)
  (road city-1-loc-46 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-46)
  (road city-1-loc-46 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-46)
  (road city-1-loc-46 city-1-loc-28)
  (road city-1-loc-28 city-1-loc-46)
  (road city-1-loc-47 city-1-loc-13)
  (road city-1-loc-13 city-1-loc-47)
  (road city-1-loc-47 city-1-loc-18)
  (road city-1-loc-18 city-1-loc-47)
  (road city-1-loc-47 city-1-loc-22)
  (road city-1-loc-22 city-1-loc-47)
  (road city-1-loc-47 city-1-loc-38)
  (road city-1-loc-38 city-1-loc-47)
  (road city-1-loc-47 city-1-loc-44)
  (road city-1-loc-44 city-1-loc-47)
  (road city-1-loc-48 city-1-loc-4)
  (road city-1-loc-4 city-1-loc-48)
  (road city-1-loc-48 city-1-loc-18)
  (road city-1-loc-18 city-1-loc-48)
  (road city-1-loc-48 city-1-loc-37)
  (road city-1-loc-37 city-1-loc-48)
  (road city-1-loc-48 city-1-loc-47)
  (road city-1-loc-47 city-1-loc-48)
  (road city-1-loc-49 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-49)
  (road city-1-loc-49 city-1-loc-25)
  (road city-1-loc-25 city-1-loc-49)
  (road city-1-loc-49 city-1-loc-27)
  (road city-1-loc-27 city-1-loc-49)
  (road city-1-loc-49 city-1-loc-35)
  (road city-1-loc-35 city-1-loc-49)
  (road city-1-loc-50 city-1-loc-2)
  (road city-1-loc-2 city-1-loc-50)
  (road city-1-loc-50 city-1-loc-34)
  (road city-1-loc-34 city-1-loc-50)
  (road city-1-loc-50 city-1-loc-43)
  (road city-1-loc-43 city-1-loc-50)
  (road city-1-loc-51 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-51)
  (road city-1-loc-52 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-52)
  (road city-1-loc-52 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-52)
  (road city-1-loc-52 city-1-loc-18)
  (road city-1-loc-18 city-1-loc-52)
  (road city-1-loc-53 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-53)
  (road city-1-loc-53 city-1-loc-31)
  (road city-1-loc-31 city-1-loc-53)
  (road city-1-loc-53 city-1-loc-32)
  (road city-1-loc-32 city-1-loc-53)
  (road city-1-loc-54 city-1-loc-6)
  (road city-1-loc-6 city-1-loc-54)
  (road city-1-loc-54 city-1-loc-16)
  (road city-1-loc-16 city-1-loc-54)
  (road city-1-loc-54 city-1-loc-36)
  (road city-1-loc-36 city-1-loc-54)
  (road city-1-loc-54 city-1-loc-46)
  (road city-1-loc-46 city-1-loc-54)
  (road city-1-loc-55 city-1-loc-7)
  (road city-1-loc-7 city-1-loc-55)
  (road city-1-loc-55 city-1-loc-17)
  (road city-1-loc-17 city-1-loc-55)
  (road city-1-loc-55 city-1-loc-20)
  (road city-1-loc-20 city-1-loc-55)
  (road city-1-loc-55 city-1-loc-24)
  (road city-1-loc-24 city-1-loc-55)
  (road city-1-loc-55 city-1-loc-32)
  (road city-1-loc-32 city-1-loc-55)
  (road city-1-loc-55 city-1-loc-53)
  (road city-1-loc-53 city-1-loc-55)
  (road city-1-loc-56 city-1-loc-1)
  (road city-1-loc-1 city-1-loc-56)
  (road city-1-loc-56 city-1-loc-11)
  (road city-1-loc-11 city-1-loc-56)
  (road city-1-loc-56 city-1-loc-33)
  (road city-1-loc-33 city-1-loc-56)
  (road city-1-loc-56 city-1-loc-52)
  (road city-1-loc-52 city-1-loc-56)
  (road city-1-loc-57 city-1-loc-10)
  (road city-1-loc-10 city-1-loc-57)
  (road city-1-loc-57 city-1-loc-12)
  (road city-1-loc-12 city-1-loc-57)
  (road city-1-loc-57 city-1-loc-25)
  (road city-1-loc-25 city-1-loc-57)
  (road city-1-loc-58 city-1-loc-3)
  (road city-1-loc-3 city-1-loc-58)
  (road city-1-loc-58 city-1-loc-12)
  (road city-1-loc-12 city-1-loc-58)
  (road city-1-loc-58 city-1-loc-25)
  (road city-1-loc-25 city-1-loc-58)
  (road city-1-loc-58 city-1-loc-49)
  (road city-1-loc-49 city-1-loc-58)
  (road city-1-loc-58 city-1-loc-57)
  (road city-1-loc-57 city-1-loc-58)
  (road city-1-loc-59 city-1-loc-9)
  (road city-1-loc-9 city-1-loc-59)
  (road city-1-loc-59 city-1-loc-15)
  (road city-1-loc-15 city-1-loc-59)
  (road city-1-loc-59 city-1-loc-19)
  (road city-1-loc-19 city-1-loc-59)
  (road city-1-loc-59 city-1-loc-23)
  (road city-1-loc-23 city-1-loc-59)
  (road city-1-loc-60 city-1-loc-8)
  (road city-1-loc-8 city-1-loc-60)
  (road city-1-loc-60 city-1-loc-29)
  (road city-1-loc-29 city-1-loc-60)
  (road city-1-loc-60 city-1-loc-51)
  (road city-1-loc-51 city-1-loc-60)
  (road city-2-loc-3 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-3)
  (road city-2-loc-4 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-4)
  (road city-2-loc-5 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-5)
  (road city-2-loc-8 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-8)
  (road city-2-loc-8 city-2-loc-4)
  (road city-2-loc-4 city-2-loc-8)
  (road city-2-loc-13 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-13)
  (road city-2-loc-13 city-2-loc-8)
  (road city-2-loc-8 city-2-loc-13)
  (road city-2-loc-15 city-2-loc-9)
  (road city-2-loc-9 city-2-loc-15)
  (road city-2-loc-19 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-19)
  (road city-2-loc-19 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-19)
  (road city-2-loc-19 city-2-loc-17)
  (road city-2-loc-17 city-2-loc-19)
  (road city-2-loc-23 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-4)
  (road city-2-loc-4 city-2-loc-23)
  (road city-2-loc-24 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-24)
  (road city-2-loc-24 city-2-loc-22)
  (road city-2-loc-22 city-2-loc-24)
  (road city-2-loc-25 city-2-loc-12)
  (road city-2-loc-12 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-25)
  (road city-2-loc-25 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-25)
  (road city-2-loc-26 city-2-loc-22)
  (road city-2-loc-22 city-2-loc-26)
  (road city-2-loc-26 city-2-loc-24)
  (road city-2-loc-24 city-2-loc-26)
  (road city-2-loc-27 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-27)
  (road city-2-loc-28 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-28)
  (road city-2-loc-28 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-28)
  (road city-2-loc-29 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-29)
  (road city-2-loc-29 city-2-loc-11)
  (road city-2-loc-11 city-2-loc-29)
  (road city-2-loc-30 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-30)
  (road city-2-loc-30 city-2-loc-26)
  (road city-2-loc-26 city-2-loc-30)
  (road city-2-loc-30 city-2-loc-28)
  (road city-2-loc-28 city-2-loc-30)
  (road city-2-loc-31 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-31)
  (road city-2-loc-32 city-2-loc-29)
  (road city-2-loc-29 city-2-loc-32)
  (road city-2-loc-33 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-33)
  (road city-2-loc-34 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-34)
  (road city-2-loc-34 city-2-loc-15)
  (road city-2-loc-15 city-2-loc-34)
  (road city-2-loc-34 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-34)
  (road city-2-loc-35 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-35)
  (road city-2-loc-36 city-2-loc-8)
  (road city-2-loc-8 city-2-loc-36)
  (road city-2-loc-36 city-2-loc-13)
  (road city-2-loc-13 city-2-loc-36)
  (road city-2-loc-36 city-2-loc-17)
  (road city-2-loc-17 city-2-loc-36)
  (road city-2-loc-37 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-37)
  (road city-2-loc-37 city-2-loc-26)
  (road city-2-loc-26 city-2-loc-37)
  (road city-2-loc-38 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-38)
  (road city-2-loc-38 city-2-loc-11)
  (road city-2-loc-11 city-2-loc-38)
  (road city-2-loc-39 city-2-loc-7)
  (road city-2-loc-7 city-2-loc-39)
  (road city-2-loc-39 city-2-loc-31)
  (road city-2-loc-31 city-2-loc-39)
  (road city-2-loc-40 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-40)
  (road city-2-loc-40 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-40)
  (road city-2-loc-40 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-40)
  (road city-2-loc-40 city-2-loc-13)
  (road city-2-loc-13 city-2-loc-40)
  (road city-2-loc-41 city-2-loc-4)
  (road city-2-loc-4 city-2-loc-41)
  (road city-2-loc-41 city-2-loc-23)
  (road city-2-loc-23 city-2-loc-41)
  (road city-2-loc-42 city-2-loc-5)
  (road city-2-loc-5 city-2-loc-42)
  (road city-2-loc-42 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-42)
  (road city-2-loc-43 city-2-loc-9)
  (road city-2-loc-9 city-2-loc-43)
  (road city-2-loc-43 city-2-loc-15)
  (road city-2-loc-15 city-2-loc-43)
  (road city-2-loc-43 city-2-loc-16)
  (road city-2-loc-16 city-2-loc-43)
  (road city-2-loc-43 city-2-loc-42)
  (road city-2-loc-42 city-2-loc-43)
  (road city-2-loc-44 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-44)
  (road city-2-loc-44 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-44)
  (road city-2-loc-44 city-2-loc-19)
  (road city-2-loc-19 city-2-loc-44)
  (road city-2-loc-44 city-2-loc-38)
  (road city-2-loc-38 city-2-loc-44)
  (road city-2-loc-45 city-2-loc-41)
  (road city-2-loc-41 city-2-loc-45)
  (road city-2-loc-46 city-2-loc-2)
  (road city-2-loc-2 city-2-loc-46)
  (road city-2-loc-46 city-2-loc-15)
  (road city-2-loc-15 city-2-loc-46)
  (road city-2-loc-46 city-2-loc-34)
  (road city-2-loc-34 city-2-loc-46)
  (road city-2-loc-46 city-2-loc-40)
  (road city-2-loc-40 city-2-loc-46)
  (road city-2-loc-46 city-2-loc-42)
  (road city-2-loc-42 city-2-loc-46)
  (road city-2-loc-47 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-47)
  (road city-2-loc-47 city-2-loc-21)
  (road city-2-loc-21 city-2-loc-47)
  (road city-2-loc-47 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-47)
  (road city-2-loc-47 city-2-loc-35)
  (road city-2-loc-35 city-2-loc-47)
  (road city-2-loc-48 city-2-loc-11)
  (road city-2-loc-11 city-2-loc-48)
  (road city-2-loc-48 city-2-loc-22)
  (road city-2-loc-22 city-2-loc-48)
  (road city-2-loc-48 city-2-loc-29)
  (road city-2-loc-29 city-2-loc-48)
  (road city-2-loc-49 city-2-loc-21)
  (road city-2-loc-21 city-2-loc-49)
  (road city-2-loc-49 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-49)
  (road city-2-loc-49 city-2-loc-47)
  (road city-2-loc-47 city-2-loc-49)
  (road city-2-loc-50 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-50)
  (road city-2-loc-50 city-2-loc-27)
  (road city-2-loc-27 city-2-loc-50)
  (road city-2-loc-51 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-51)
  (road city-2-loc-51 city-2-loc-21)
  (road city-2-loc-21 city-2-loc-51)
  (road city-2-loc-51 city-2-loc-35)
  (road city-2-loc-35 city-2-loc-51)
  (road city-2-loc-52 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-52)
  (road city-2-loc-52 city-2-loc-39)
  (road city-2-loc-39 city-2-loc-52)
  (road city-2-loc-53 city-2-loc-6)
  (road city-2-loc-6 city-2-loc-53)
  (road city-2-loc-53 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-53)
  (road city-2-loc-53 city-2-loc-38)
  (road city-2-loc-38 city-2-loc-53)
  (road city-2-loc-54 city-2-loc-12)
  (road city-2-loc-12 city-2-loc-54)
  (road city-2-loc-55 city-2-loc-18)
  (road city-2-loc-18 city-2-loc-55)
  (road city-2-loc-55 city-2-loc-30)
  (road city-2-loc-30 city-2-loc-55)
  (road city-2-loc-55 city-2-loc-35)
  (road city-2-loc-35 city-2-loc-55)
  (road city-2-loc-55 city-2-loc-51)
  (road city-2-loc-51 city-2-loc-55)
  (road city-2-loc-56 city-2-loc-9)
  (road city-2-loc-9 city-2-loc-56)
  (road city-2-loc-56 city-2-loc-15)
  (road city-2-loc-15 city-2-loc-56)
  (road city-2-loc-56 city-2-loc-43)
  (road city-2-loc-43 city-2-loc-56)
  (road city-2-loc-57 city-2-loc-20)
  (road city-2-loc-20 city-2-loc-57)
  (road city-2-loc-57 city-2-loc-32)
  (road city-2-loc-32 city-2-loc-57)
  (road city-2-loc-57 city-2-loc-48)
  (road city-2-loc-48 city-2-loc-57)
  (road city-2-loc-57 city-2-loc-50)
  (road city-2-loc-50 city-2-loc-57)
  (road city-2-loc-58 city-2-loc-54)
  (road city-2-loc-54 city-2-loc-58)
  (road city-2-loc-59 city-2-loc-14)
  (road city-2-loc-14 city-2-loc-59)
  (road city-2-loc-59 city-2-loc-26)
  (road city-2-loc-26 city-2-loc-59)
  (road city-2-loc-59 city-2-loc-28)
  (road city-2-loc-28 city-2-loc-59)
  (road city-2-loc-59 city-2-loc-30)
  (road city-2-loc-30 city-2-loc-59)
  (road city-2-loc-59 city-2-loc-37)
  (road city-2-loc-37 city-2-loc-59)
  (road city-2-loc-60 city-2-loc-1)
  (road city-2-loc-1 city-2-loc-60)
  (road city-2-loc-60 city-2-loc-3)
  (road city-2-loc-3 city-2-loc-60)
  (road city-2-loc-60 city-2-loc-10)
  (road city-2-loc-10 city-2-loc-60)
  (road city-2-loc-60 city-2-loc-38)
  (road city-2-loc-38 city-2-loc-60)
  (road city-2-loc-60 city-2-loc-44)
  (road city-2-loc-44 city-2-loc-60)
  (road city-3-loc-9 city-3-loc-1)
  (road city-3-loc-1 city-3-loc-9)
  (road city-3-loc-9 city-3-loc-3)
  (road city-3-loc-3 city-3-loc-9)
  (road city-3-loc-11 city-3-loc-4)
  (road city-3-loc-4 city-3-loc-11)
  (road city-3-loc-13 city-3-loc-8)
  (road city-3-loc-8 city-3-loc-13)
  (road city-3-loc-14 city-3-loc-2)
  (road city-3-loc-2 city-3-loc-14)
  (road city-3-loc-16 city-3-loc-1)
  (road city-3-loc-1 city-3-loc-16)
  (road city-3-loc-16 city-3-loc-7)
  (road city-3-loc-7 city-3-loc-16)
  (road city-3-loc-18 city-3-loc-15)
  (road city-3-loc-15 city-3-loc-18)
  (road city-3-loc-19 city-3-loc-4)
  (road city-3-loc-4 city-3-loc-19)
  (road city-3-loc-22 city-3-loc-17)
  (road city-3-loc-17 city-3-loc-22)
  (road city-3-loc-23 city-3-loc-5)
  (road city-3-loc-5 city-3-loc-23)
  (road city-3-loc-23 city-3-loc-17)
  (road city-3-loc-17 city-3-loc-23)
  (road city-3-loc-23 city-3-loc-22)
  (road city-3-loc-22 city-3-loc-23)
  (road city-3-loc-24 city-3-loc-20)
  (road city-3-loc-20 city-3-loc-24)
  (road city-3-loc-25 city-3-loc-5)
  (road city-3-loc-5 city-3-loc-25)
  (road city-3-loc-25 city-3-loc-13)
  (road city-3-loc-13 city-3-loc-25)
  (road city-3-loc-26 city-3-loc-13)
  (road city-3-loc-13 city-3-loc-26)
  (road city-3-loc-27 city-3-loc-26)
  (road city-3-loc-26 city-3-loc-27)
  (road city-3-loc-28 city-3-loc-24)
  (road city-3-loc-24 city-3-loc-28)
  (road city-3-loc-29 city-3-loc-15)
  (road city-3-loc-15 city-3-loc-29)
  (road city-3-loc-29 city-3-loc-18)
  (road city-3-loc-18 city-3-loc-29)
  (road city-3-loc-30 city-3-loc-6)
  (road city-3-loc-6 city-3-loc-30)
  (road city-3-loc-30 city-3-loc-26)
  (road city-3-loc-26 city-3-loc-30)
  (road city-3-loc-31 city-3-loc-3)
  (road city-3-loc-3 city-3-loc-31)
  (road city-3-loc-32 city-3-loc-2)
  (road city-3-loc-2 city-3-loc-32)
  (road city-3-loc-32 city-3-loc-14)
  (road city-3-loc-14 city-3-loc-32)
  (road city-3-loc-33 city-3-loc-21)
  (road city-3-loc-21 city-3-loc-33)
  (road city-3-loc-33 city-3-loc-25)
  (road city-3-loc-25 city-3-loc-33)
  (road city-3-loc-34 city-3-loc-2)
  (road city-3-loc-2 city-3-loc-34)
  (road city-3-loc-34 city-3-loc-6)
  (road city-3-loc-6 city-3-loc-34)
  (road city-3-loc-34 city-3-loc-32)
  (road city-3-loc-32 city-3-loc-34)
  (road city-3-loc-36 city-3-loc-3)
  (road city-3-loc-3 city-3-loc-36)
  (road city-3-loc-36 city-3-loc-31)
  (road city-3-loc-31 city-3-loc-36)
  (road city-3-loc-37 city-3-loc-5)
  (road city-3-loc-5 city-3-loc-37)
  (road city-3-loc-37 city-3-loc-8)
  (road city-3-loc-8 city-3-loc-37)
  (road city-3-loc-37 city-3-loc-22)
  (road city-3-loc-22 city-3-loc-37)
  (road city-3-loc-37 city-3-loc-23)
  (road city-3-loc-23 city-3-loc-37)
  (road city-3-loc-38 city-3-loc-1)
  (road city-3-loc-1 city-3-loc-38)
  (road city-3-loc-38 city-3-loc-3)
  (road city-3-loc-3 city-3-loc-38)
  (road city-3-loc-38 city-3-loc-7)
  (road city-3-loc-7 city-3-loc-38)
  (road city-3-loc-38 city-3-loc-9)
  (road city-3-loc-9 city-3-loc-38)
  (road city-3-loc-38 city-3-loc-36)
  (road city-3-loc-36 city-3-loc-38)
  (road city-3-loc-39 city-3-loc-26)
  (road city-3-loc-26 city-3-loc-39)
  (road city-3-loc-39 city-3-loc-27)
  (road city-3-loc-27 city-3-loc-39)
  (road city-3-loc-39 city-3-loc-32)
  (road city-3-loc-32 city-3-loc-39)
  (road city-3-loc-40 city-3-loc-20)
  (road city-3-loc-20 city-3-loc-40)
  (road city-3-loc-40 city-3-loc-24)
  (road city-3-loc-24 city-3-loc-40)
  (road city-3-loc-40 city-3-loc-28)
  (road city-3-loc-28 city-3-loc-40)
  (road city-3-loc-41 city-3-loc-1)
  (road city-3-loc-1 city-3-loc-41)
  (road city-3-loc-41 city-3-loc-16)
  (road city-3-loc-16 city-3-loc-41)
  (road city-3-loc-41 city-3-loc-27)
  (road city-3-loc-27 city-3-loc-41)
  (road city-3-loc-42 city-3-loc-20)
  (road city-3-loc-20 city-3-loc-42)
  (road city-3-loc-42 city-3-loc-22)
  (road city-3-loc-22 city-3-loc-42)
  (road city-3-loc-42 city-3-loc-24)
  (road city-3-loc-24 city-3-loc-42)
  (road city-3-loc-42 city-3-loc-37)
  (road city-3-loc-37 city-3-loc-42)
  (road city-3-loc-43 city-3-loc-6)
  (road city-3-loc-6 city-3-loc-43)
  (road city-3-loc-43 city-3-loc-28)
  (road city-3-loc-28 city-3-loc-43)
  (road city-3-loc-44 city-3-loc-6)
  (road city-3-loc-6 city-3-loc-44)
  (road city-3-loc-44 city-3-loc-24)
  (road city-3-loc-24 city-3-loc-44)
  (road city-3-loc-44 city-3-loc-43)
  (road city-3-loc-43 city-3-loc-44)
  (road city-3-loc-45 city-3-loc-10)
  (road city-3-loc-10 city-3-loc-45)
  (road city-3-loc-45 city-3-loc-14)
  (road city-3-loc-14 city-3-loc-45)
  (road city-3-loc-46 city-3-loc-10)
  (road city-3-loc-10 city-3-loc-46)
  (road city-3-loc-46 city-3-loc-12)
  (road city-3-loc-12 city-3-loc-46)
  (road city-3-loc-46 city-3-loc-14)
  (road city-3-loc-14 city-3-loc-46)
  (road city-3-loc-46 city-3-loc-45)
  (road city-3-loc-45 city-3-loc-46)
  (road city-3-loc-47 city-3-loc-12)
  (road city-3-loc-12 city-3-loc-47)
  (road city-3-loc-47 city-3-loc-14)
  (road city-3-loc-14 city-3-loc-47)
  (road city-3-loc-47 city-3-loc-32)
  (road city-3-loc-32 city-3-loc-47)
  (road city-3-loc-47 city-3-loc-39)
  (road city-3-loc-39 city-3-loc-47)
  (road city-3-loc-48 city-3-loc-4)
  (road city-3-loc-4 city-3-loc-48)
  (road city-3-loc-48 city-3-loc-11)
  (road city-3-loc-11 city-3-loc-48)
  (road city-3-loc-48 city-3-loc-19)
  (road city-3-loc-19 city-3-loc-48)
  (road city-3-loc-48 city-3-loc-35)
  (road city-3-loc-35 city-3-loc-48)
  (road city-3-loc-49 city-3-loc-33)
  (road city-3-loc-33 city-3-loc-49)
  (road city-3-loc-49 city-3-loc-35)
  (road city-3-loc-35 city-3-loc-49)
  (road city-3-loc-49 city-3-loc-48)
  (road city-3-loc-48 city-3-loc-49)
  (road city-3-loc-50 city-3-loc-5)
  (road city-3-loc-5 city-3-loc-50)
  (road city-3-loc-50 city-3-loc-19)
  (road city-3-loc-19 city-3-loc-50)
  (road city-3-loc-50 city-3-loc-25)
  (road city-3-loc-25 city-3-loc-50)
  (road city-3-loc-50 city-3-loc-33)
  (road city-3-loc-33 city-3-loc-50)
  (road city-3-loc-50 city-3-loc-49)
  (road city-3-loc-49 city-3-loc-50)
  (road city-3-loc-51 city-3-loc-12)
  (road city-3-loc-12 city-3-loc-51)
  (road city-3-loc-51 city-3-loc-16)
  (road city-3-loc-16 city-3-loc-51)
  (road city-3-loc-52 city-3-loc-1)
  (road city-3-loc-1 city-3-loc-52)
  (road city-3-loc-52 city-3-loc-9)
  (road city-3-loc-9 city-3-loc-52)
  (road city-3-loc-52 city-3-loc-21)
  (road city-3-loc-21 city-3-loc-52)
  (road city-3-loc-52 city-3-loc-27)
  (road city-3-loc-27 city-3-loc-52)
  (road city-3-loc-53 city-3-loc-15)
  (road city-3-loc-15 city-3-loc-53)
  (road city-3-loc-53 city-3-loc-29)
  (road city-3-loc-29 city-3-loc-53)
  (road city-3-loc-53 city-3-loc-33)
  (road city-3-loc-33 city-3-loc-53)
  (road city-3-loc-53 city-3-loc-35)
  (road city-3-loc-35 city-3-loc-53)
  (road city-3-loc-53 city-3-loc-49)
  (road city-3-loc-49 city-3-loc-53)
  (road city-3-loc-54 city-3-loc-13)
  (road city-3-loc-13 city-3-loc-54)
  (road city-3-loc-54 city-3-loc-21)
  (road city-3-loc-21 city-3-loc-54)
  (road city-3-loc-54 city-3-loc-26)
  (road city-3-loc-26 city-3-loc-54)
  (road city-3-loc-54 city-3-loc-27)
  (road city-3-loc-27 city-3-loc-54)
  (road city-3-loc-54 city-3-loc-52)
  (road city-3-loc-52 city-3-loc-54)
  (road city-3-loc-55 city-3-loc-28)
  (road city-3-loc-28 city-3-loc-55)
  (road city-3-loc-55 city-3-loc-40)
  (road city-3-loc-40 city-3-loc-55)
  (road city-3-loc-56 city-3-loc-4)
  (road city-3-loc-4 city-3-loc-56)
  (road city-3-loc-56 city-3-loc-17)
  (road city-3-loc-17 city-3-loc-56)
  (road city-3-loc-56 city-3-loc-23)
  (road city-3-loc-23 city-3-loc-56)
  (road city-3-loc-57 city-3-loc-6)
  (road city-3-loc-6 city-3-loc-57)
  (road city-3-loc-57 city-3-loc-8)
  (road city-3-loc-8 city-3-loc-57)
  (road city-3-loc-57 city-3-loc-30)
  (road city-3-loc-30 city-3-loc-57)
  (road city-3-loc-57 city-3-loc-44)
  (road city-3-loc-44 city-3-loc-57)
  (road city-3-loc-58 city-3-loc-3)
  (road city-3-loc-3 city-3-loc-58)
  (road city-3-loc-58 city-3-loc-18)
  (road city-3-loc-18 city-3-loc-58)
  (road city-3-loc-59 city-3-loc-11)
  (road city-3-loc-11 city-3-loc-59)
  (road city-3-loc-59 city-3-loc-48)
  (road city-3-loc-48 city-3-loc-59)
  (road city-3-loc-60 city-3-loc-3)
  (road city-3-loc-3 city-3-loc-60)
  (road city-3-loc-60 city-3-loc-9)
  (road city-3-loc-9 city-3-loc-60)
  (road city-3-loc-60 city-3-loc-18)
  (road city-3-loc-18 city-3-loc-60)
  (road city-3-loc-60 city-3-loc-38)
  (road city-3-loc-38 city-3-loc-60)
  (road city-3-loc-60 city-3-loc-58)
  (road city-3-loc-58 city-3-loc-60)
  (road city-1-loc-39 city-2-loc-33)
  (road city-2-loc-33 city-1-loc-39)
  (road city-1-loc-59 city-3-loc-58)
  (road city-3-loc-58 city-1-loc-59)
  (road city-2-loc-49 city-3-loc-21)
  (road city-3-loc-21 city-2-loc-49)
  (at package-1 city-1-loc-20)
  (at package-2 city-2-loc-8)
  (at package-3 city-1-loc-8)
  (at package-4 city-2-loc-27)
  (at package-5 city-3-loc-8)
  (at package-6 city-1-loc-9)
  (at package-7 city-1-loc-35)
  (at package-8 city-3-loc-8)
  (at package-9 city-3-loc-50)
  (at package-10 city-2-loc-17)
  (at package-11 city-1-loc-52)
  (at package-12 city-1-loc-51)
  (at package-13 city-1-loc-7)
  (at package-14 city-2-loc-42)
  (at package-15 city-2-loc-36)
  (at package-16 city-2-loc-27)
  (at package-17 city-1-loc-18)
  (at package-18 city-1-loc-43)
  (at package-19 city-2-loc-46)
  (at package-20 city-2-loc-49)
  (at package-21 city-2-loc-4)
  (at package-22 city-3-loc-20)
  (at truck-1 city-3-loc-1)
  (capacity truck-1 capacity-2)
  (at truck-2 city-2-loc-30)
  (capacity truck-2 capacity-2)
  (at truck-3 city-2-loc-46)
  (capacity truck-3 capacity-2)
  (at truck-4 city-3-loc-18)
  (capacity truck-4 capacity-4)
 )
)
