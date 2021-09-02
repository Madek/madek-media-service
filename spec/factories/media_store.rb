FactoryGirl.define do
  factory :media_store do
    id { Faker::Lorem.words(2).join("-") }
    description { Faker::Lorem.sentence }
    configuration {}
    type { MediaStore::TYPES.without("database").sample }

    trait :database do
      id { "database" }
      type { "database" }
    end

    trait :with_users do
      transient { users { [] } }

      after(:create) do |store, evaluator|
        store.users << evaluator.users
      end
    end

    trait :with_groups do
      transient { groups { [] } }

      after(:create) do |store, evaluator|
        store.groups << evaluator.groups
      end
    end
  end
end
