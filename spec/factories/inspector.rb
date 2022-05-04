FactoryGirl.define do
  factory :inspector do
    id {Faker::Internet.domain_word + ".inspector.example.com" }
    description { Faker::Lorem.sentence }
    enabled {false}
  end
end
