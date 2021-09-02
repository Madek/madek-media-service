FactoryGirl.modify do
  factory :user do
    trait :with_admin_role do
      after(:create) do |user, _|
        create(:admin, user: user)
      end
    end

    trait :with_system_admin_role do
      after(:create) do |user, _|
        create(:system_admin, user: user)
      end
    end
  end
end
